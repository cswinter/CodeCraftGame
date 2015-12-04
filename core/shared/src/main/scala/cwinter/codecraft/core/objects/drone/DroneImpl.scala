package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core._
import cwinter.codecraft.core.api.{Player, DroneControllerBase, DroneSpec, MineralCrystal}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.objects.{EnergyGlobeObject, MineralCrystalImpl, WorldObject}
import cwinter.codecraft.core.replay._
import cwinter.codecraft.graphics.worldstate.{DroneDescriptor, DroneModuleDescriptor, WorldObjectDescriptor}
import cwinter.codecraft.util.maths.{Float0To1, Vector2}


private[core] class DroneImpl(
  val spec: DroneSpec,
  val controller: DroneControllerBase,
  val player: Player,
  initialPos: Vector2,
  time: Double,
  val worldConfig: WorldConfig,
  val replayRecorder: ReplayRecorder = NullReplayRecorder,
  startingResources: Int = 0
) extends WorldObject {
  require(worldConfig != null)

  var objectsInSight: Set[WorldObject] = Set.empty[WorldObject]

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)
  private[this] var hullState = List.fill[Byte](spec.size - 1)(2)
  private[core] var constructionProgress: Option[Int] = None
  private[this] var mineralDepositee: Option[DroneImpl] = None
  private[this] var _hasDied: Boolean = false
  private[this] var automaticMineralProcessing: Boolean = true

  final val MessageCooldown = 30
  private[this] var messageCooldown = 0
  private final val NJetPositions = 6
  private[this] val oldPositions = collection.mutable.Queue.empty[(Float, Float, Float)]


  // TODO: remove this once all logic is moved into modules
  private[this] var simulatorEvents = List.empty[SimulatorEvent]

  val dynamics: DroneDynamics = spec.constructDynamics(this, initialPos, time)
  private[this] val weapons = spec.constructMissilesBatteries(this)
  private[this] val refineries = spec.constructRefineries(this)
  private[core] val storage = spec.constructStorage(this, startingResources)
  private[this] val manipulator = spec.constructManipulatorModules(this)
  private[this] val shieldGenerators = spec.constructShieldGenerators(this)
  private[this] val engines = spec.constructEngineModules(this)
  val droneModules = Seq(weapons, refineries, storage, manipulator, shieldGenerators, engines)


  def initialise(time: Double): Unit = {
    dynamics.setTime(time)
    controller.initialise(this)
  }

  def processEvents(): Unit = {
    controller.willProcessEvents()

    for (mineralCrystal <- storedMinerals) {
      if (availableFactories >= mineralCrystal.size) {
        startMineralProcessing(mineralCrystal)
      }
    }

    for (event <- dynamics.arrivalEvent) {
      enqueueEvent(event)
    }

    if (isDead) {
      // TODO: think about the semantics of thsi
      controller.onDeath()
      for (s <- storage) s.destroyed()
    } else {
      // process events
      eventQueue foreach {
        case Spawned => controller.onSpawn()
        case Destroyed => // this should never be executed
        case MineralEntersSightRadius(mineral) =>
          controller.onMineralEntersVision(new MineralCrystal(mineral, player))
        case ArrivedAtPosition => controller.onArrivesAtPosition()
        case ArrivedAtDrone(drone) =>
          val droneHandle = if (drone.player == player) drone.controller else new EnemyDrone(drone, player)
          controller.onArrivesAtDrone(droneHandle)
        case ArrivedAtMineral(mineral) =>
          controller.onArrivesAtMineral(new MineralCrystal(mineral, player))
        case DroneEntersSightRadius(drone) => controller.onDroneEntersVision(
          if (drone.player == player) drone.controller
          else new EnemyDrone(drone, player)
        )
        case event => throw new Exception(s"Unhandled event! $event")
      }
      eventQueue.clear()
      controller.onTick()
    }
  }

  override def update(): Seq[SimulatorEvent] = {
    for (depositee <- mineralDepositee) {
      val capacity = depositee.availableStorage
      val required = storedMinerals.minBy(_.size).size
      if (capacity < required) {
        inform(s"Cannot deposit minerals - only $capacity free storage available. Required: $required")
      } else {
        for {
          s <- storage
          (min, pos) <- s.popMineralCrystal(capacity)
        } {
          depositee.depositMineral(min, pos)
          if (s.storedMinerals.isEmpty) {
            mineralDepositee = None
          }
        }
      }
    }

    for (Some(m) <- droneModules) {
      val (events, resourceDepletions, resourceSpawns) = m.update(availableResources)
      simulatorEvents :::= events.toList
      for {
        s <- storage
        rd <- resourceDepletions
        pos = s.withdrawEnergyGlobe()
      } simulatorEvents ::= SpawnEnergyGlobeAnimation(new EnergyGlobeObject(pos, 30, rd))
      for (s <- storage; rs <- resourceSpawns) s.depositEnergyGlobe(rs)
    }
    dynamics.update()

    messageCooldown -= 1

    oldPositions.enqueue((position.x.toFloat, position.y.toFloat, dynamics.orientation.toFloat))
    if (oldPositions.length > NJetPositions) oldPositions.dequeue()

    val events = simulatorEvents
    simulatorEvents = List.empty[SimulatorEvent]
    events
  }

  def enqueueEvent(event: DroneEvent): Unit = {
    eventQueue.enqueue(event)
  }

  def missileHit(position: Vector2): Unit = {
    def damageHull(hull: List[Byte]): List[Byte] = hull match {
      case h :: hs =>
        if (h > 0) (h - 1).toByte :: hs
        else h :: damageHull(hs)
      case Nil => Nil
    }


    val incomingDamage = 1
    val damage = shieldGenerators.map(_.absorbDamage(incomingDamage)).getOrElse(incomingDamage)

    for (_ <- 0 until damage)
      hullState = damageHull(hullState)

    if (hitpoints == 0) {
      dynamics.remove()
      simulatorEvents ::= DroneKilled(this)
      _hasDied = true
      for {
        m <- manipulator
        d <- m.droneInConstruction
      } simulatorEvents ::= DroneConstructionCancelled(d)
      for {
        f <- refineries
        c <- f.mineralCrystals
      } simulatorEvents ::= MineralCrystalDestroyed(c)
    }
  }

  @inline final def !(command: DroneCommand) = executeCommand(command)
  
  def executeCommand(command: DroneCommand) = {
    var redundant = false
    command match {
      case mc: MovementCommand =>
        redundant = giveMovementCommand(mc)
      case cd: ConstructDrone => startDroneConstruction(cd)
      case DepositMinerals(target) => depositMinerals(target)
      case FireMissiles(target) => fireWeapons(target)
      case HarvestMineral(mineral) => harvestResource(mineral)
      case ProcessMineral(mineral) => startMineralProcessing(mineral)
    }
    if (!redundant) {
      replayRecorder.record(id, command)
    }
  }
  

  //+------------------------------+
  //+ Command implementations      +
  //+------------------------------+
  private def giveMovementCommand(value: MovementCommand): Boolean = {
    dynamics.setMovementCommand(value)
  }

  private def startDroneConstruction(command: ConstructDrone): Unit = {
    manipulator match {
      case Some(m) => m.startDroneConstruction(command)
      case None => warn("Drone construction requires a constructor module.")
    }
  }

  def immobile = droneModules.exists(_.exists(_.cancelMovement)) || mineralDepositee.isDefined


  def depositMineral(crystal: MineralCrystalImpl, pos: Vector2): Unit = {
    for {
      s <- storage
    } s.depositMineral(crystal, pos)
  }

  private def startMineralProcessing(mineral: MineralCrystalImpl): Unit = {
    if (!storage.exists(_.storedMinerals.contains(mineral))) {
      warn("Tried to process mineral not stored in this drone!")
    } else {
      refineries match {
        case Some(f) =>
          storage.get.removeMineralCrystal(mineral)
          f.startMineralProcessing(mineral)
        case None => warn("Processing minerals requires a refinery module.")
      }
    }
  }

  private def fireWeapons(target: DroneImpl): Unit = {
    if (target == this) {
      warn("Drone tried to shoot itself!")
    } else {
      weapons match {
        case Some(w) => w.fire(target)
        case None => warn("Firing missiles requires a missile battery module.")
      }
    }
  }

  private def harvestResource(mineralCrystal: MineralCrystalImpl): Unit = {
    storage match {
      case Some(s) => s.harvestMineral(mineralCrystal)
      case None => warn("Harvesting resources requires a storage module.")
    }
  }

  private def depositMinerals(other: DroneImpl): Unit = {
    if (other == this) {
      warn("Drone is trying to deposit minerals into itself!")
    } else if (other.storage.isEmpty) {
      warn("Trying to deposit minerals into a drone without a storage module.")
    } else if (storedMinerals.isEmpty) {
      warn("Drone has no minerals to deposit.")
    } else if ((other.position - position).lengthSquared > (radius + other.radius + 12) * (radius + other.radius + 12)) {
      warn("Too far away to deposit minerals.")
    } else {
      mineralDepositee = Some(other)
    }
  }


  //+------------------------------+
  //| Drone properties             +
  //+------------------------------+
  override def position: Vector2 = dynamics.pos
  def weaponsCooldown: Int = weapons.map(_.cooldown).getOrElse(1)
  def hitpoints: Int = hullState.map(_.toInt).sum
  def dronesInSight: Set[DroneImpl] = objectsInSight.filter(_.isInstanceOf[DroneImpl]).map { case d: DroneImpl => d }
  def isConstructing: Boolean = manipulator.exists(_.isConstructing)
  def storageCapacity = spec.storageModules
  def processingCapacity = spec.refineries
  def size = spec.size
  def radius = spec.radius

  def availableStorage: Int = {
    for (s <- storage) yield s.availableStorage
  }.getOrElse(0)

  def availableFactories: Int = {
    for (f <- refineries) yield f.currentCapacity
  }.getOrElse(0)

  def availableResources: Int = {
    for (s <- storage) yield s.availableResources
  }.getOrElse(0)

  def totalAvailableResources: Int = {
    for (s <- storage) yield s.totalAvailableResources(processingCapacity)
  }.getOrElse(0) + {
    for (r <- refineries) yield r.unprocessedResourceAmount
  }.getOrElse(0)

  def storedMinerals: Iterable[MineralCrystalImpl] = {
    for (s <- storage) yield s.storedMinerals
  }.getOrElse(Seq())

  override def descriptor: Seq[WorldObjectDescriptor] =
  {
    Seq(
      DroneDescriptor(
        id,
        position.x.toFloat,
        position.y.toFloat,
        dynamics.orientation.toFloat,
        Seq(), //oldPositions :+ (position.x.toFloat, position.y.toFloat, dynamics.orientation.toFloat),
        moduleDescriptors,
        hullState,
        shieldGenerators.map(_.hitpointPercentage),
        spec.size,
        player.color,
        constructionProgress.map(p => Float0To1(p / spec.buildTime.toFloat))
      )) ++ manipulator.toSeq.flatMap(_.manipulatorGraphics) ++
      storage.toSeq.flatMap(_.energyGlobeAnimations)
  }

  private def moduleDescriptors: Seq[DroneModuleDescriptor] =
  {
    for {
      Some(m) <- droneModules
      descr <- m.descriptors
    } yield descr
  }

  def error(message: String): Unit = {
    if (messageCooldown <= 0) {
      messageCooldown = MessageCooldown
      Errors.addMessage(message, position, errors.Error)
    }
  }

  def warn(message: String): Unit = {
    if (messageCooldown <= 0) {
      messageCooldown = MessageCooldown
      Errors.warn(message, position)
    }
  }

  def inform(message: String): Unit = {
    if (messageCooldown <= 0) {
      messageCooldown = MessageCooldown
      Errors.inform(message, position)
    }
  }

  override def isDead = _hasDied

  override def toString: String = id.toString
}


private[core] sealed trait DroneEvent
private[core] case object Spawned extends DroneEvent
private[core] case object Destroyed extends DroneEvent
private[core] case class MineralEntersSightRadius(mineralCrystal: MineralCrystalImpl) extends DroneEvent
private[core] case object ArrivedAtPosition extends DroneEvent
private[core] case class ArrivedAtMineral(mineral: MineralCrystalImpl) extends DroneEvent
private[core] case class ArrivedAtDrone(drone: DroneImpl) extends DroneEvent
private[core] case class DroneEntersSightRadius(drone: DroneImpl) extends DroneEvent


import upickle.default.key
private[core] sealed trait SerializableDroneCommand
@key("Construct") private[core] case class SerializableConstructDrone(spec: DroneSpec, position: Vector2) extends SerializableDroneCommand
@key("Process") private[core] case class SerializableProcessMineral(mineralID: Int) extends SerializableDroneCommand
@key("FireMissiles") private[core] case class SerializableFireMissiles(targetID: Int) extends SerializableDroneCommand
@key("Deposit") private[core] case class SerializableDepositMinerals(targetID: Int) extends SerializableDroneCommand
@key("Harvest") private[core] case class SerializableHarvestMineral(mineralID: Int) extends SerializableDroneCommand
@key("MoveToMineral") private[core] case class SerializableMoveToMineralCrystal(mineralCrystalID: Int) extends SerializableDroneCommand
@key("MoveToDrone") private[core] case class SerializableMoveToDrone(droneID: Int) extends SerializableDroneCommand


private[core] sealed trait DroneCommand {
  def toSerializable: SerializableDroneCommand
}
object DroneCommand {
  def apply(
    serialized: SerializableDroneCommand
  )(implicit droneRegistry: Map[Int, DroneImpl], mineralRegistry: Map[Int, MineralCrystalImpl]): DroneCommand = {
    import scala.language.implicitConversions
    implicit def droneLookup(droneID: Int): DroneImpl = {
      if (droneRegistry.contains(droneID)) droneRegistry(droneID)
      else throw new Exception(f"Cannot find drone with id $droneID. Available IDs: ${droneRegistry.keys}")
    }
    implicit def mineralLookup(mineralID: Int): MineralCrystalImpl = {
      if (mineralRegistry.contains(mineralID)) mineralRegistry(mineralID)
      else throw new Exception(f"Cannot find mineral with id $mineralID. Available IDs: ${mineralRegistry.keys}")
    }
    serialized match {
      case SerializableConstructDrone(spec, position) => ConstructDrone(spec, new DummyDroneController, position)
      case SerializableProcessMineral(mineral) => ProcessMineral(mineral)
      case SerializableFireMissiles(target) => FireMissiles(target)
      case SerializableDepositMinerals(target) => DepositMinerals(target)
      case SerializableHarvestMineral(mineral) => HarvestMineral(mineral)
      case SerializableMoveToMineralCrystal(mineral) => MoveToMineralCrystal(mineral)
      case SerializableMoveToDrone(drone) => MoveToDrone(drone)
      case move: MoveInDirection => move
      case move: MoveToPosition => move
      case HoldPosition => HoldPosition
    }
  }
}
private[core] case class ConstructDrone(
  spec: DroneSpec,
  controller: DroneControllerBase,
  position: Vector2
) extends DroneCommand {
  def toSerializable = SerializableConstructDrone(spec, position)
}
private[core] case class ProcessMineral(mineralCrystal: MineralCrystalImpl) extends DroneCommand {
  def toSerializable = SerializableProcessMineral(mineralCrystal.id)
}
private[core] case class FireMissiles(target: DroneImpl) extends DroneCommand {
  def toSerializable = SerializableFireMissiles(target.id)
}
private[core] case class DepositMinerals(target: DroneImpl) extends DroneCommand {
  def toSerializable = SerializableDepositMinerals(target.id)
}
private[core] case class HarvestMineral(mineral: MineralCrystalImpl) extends DroneCommand {
  def toSerializable = SerializableHarvestMineral(mineral.id)
}

private[core] sealed trait MovementCommand extends DroneCommand
@key("MoveInDirec") private[core] case class MoveInDirection(direction: Double) extends MovementCommand with SerializableDroneCommand {
  def toSerializable = this
}
@key("MoveToPos") private[core] case class MoveToPosition(position: Vector2) extends MovementCommand with SerializableDroneCommand {
  def toSerializable = this
}
private[core] case class MoveToMineralCrystal(mineralCrystal: MineralCrystalImpl) extends MovementCommand {
  def toSerializable = SerializableMoveToMineralCrystal(mineralCrystal.id)
}
private[core] case class MoveToDrone(drone: DroneImpl) extends MovementCommand {
  def toSerializable = SerializableMoveToDrone(drone.id)
}
@key("Stop") private[core] case object HoldPosition extends MovementCommand with SerializableDroneCommand {
  def toSerializable = this
}


