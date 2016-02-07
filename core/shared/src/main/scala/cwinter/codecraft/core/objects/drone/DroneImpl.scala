package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core._
import cwinter.codecraft.core.api.{Player, DroneControllerBase, DroneSpec, MineralCrystal}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.objects._
import cwinter.codecraft.core.replay._
import cwinter.codecraft.graphics.worldstate.{DroneDescriptor, DroneModuleDescriptor, WorldObjectDescriptor}
import cwinter.codecraft.util.maths.{Rectangle, Float0To1, Vector2}


private[core] class DroneImpl(
  val spec: DroneSpec,
  val controller: DroneControllerBase,
  val context: DroneContext,
  initialPos: Vector2,
  time: Double,
  startingResources: Int = 0
) extends WorldObject {
  require(context.worldConfig != null)

  val id = context.idGenerator.getAndIncrement()

  var objectsInSight: Set[WorldObject] = Set.empty[WorldObject]

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  // TODO: move all this state into submodules?
  private[this] var hullState = List.fill[Byte](spec.size - 1)(2)
  private[core] var constructionProgress: Option[Int] = None
  private[this] var _hasDied: Boolean = false

  private[this] var _missileHits = List.empty[MissileHit]
  def popMissileHits(): Seq[MissileHit] = {
    val result = _missileHits
    _missileHits = List.empty[MissileHit]
    result
  }

  final val MessageCooldown = 30
  private[this] var messageCooldown = 0
  private final val NJetPositions = 6
  private[this] val oldPositions = collection.mutable.Queue.empty[(Float, Float, Float)]


  // TODO: remove this once all logic is moved into modules
  private[this] var simulatorEvents = List.empty[SimulatorEvent]

  val dynamics: DroneDynamics = spec.constructDynamics(this, initialPos, time)
  private[this] val weapons = spec.constructMissilesBatteries(this)
  private[core] val storage = spec.constructStorage(this, startingResources)
  private[this] val manipulator = spec.constructManipulatorModules(this)
  private[this] val shieldGenerators = spec.constructShieldGenerators(this)
  private[this] val engines = spec.constructEngineModules(this)
  val droneModules = Seq(weapons, storage, manipulator, shieldGenerators, engines)


  def initialise(time: Double): Unit = {
    dynamics.setTime(time)
    controller.initialise(this)
  }

  def processEvents(): Unit = {
    controller.willProcessEvents()

    for (event <- dynamics.checkArrivalConditions()) {
      enqueueEvent(event)
    }

    if (isDead) {
      controller.onDeath()
    } else {
      // process events
      eventQueue foreach {
        case Destroyed => // this should never be executed
        case MineralEntersSightRadius(mineral) =>
          controller.onMineralEntersVision(mineral.getHandle(player))
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
        case Spawned => /* this is handled by simulator to ensure onSpawn is called before any other events */
        case event => throw new Exception(s"Unhandled event! $event")
      }
      eventQueue.clear()
      controller.onTick()
    }
  }

  override def update(): Seq[SimulatorEvent] = {
     for(Some(m) <- droneModules) {
      val (events, resourceDepletions, resourceSpawns) = m.update(storedResources)
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

  // TODO: mb only record hits here and do all processing as part of update()
  // NOTE: death state probable must be determined before (or at very start of) next update()
  def missileHit(missile: HomingMissile): Unit = {
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
      for (s <- storage) s.droneHasDied()
    }

    // TODO: only do this in multiplayer games
    if (context.isLocallyComputed) {
      _missileHits ::= MissileHit(id, missile.position, missile.id)
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
    }
    if (!redundant) {
      context.replayRecorder.record(id, command)
      context.commandRecorder.foreach(_.record(id, command))
    }
  }

  def applyState(state: DroneDynamicsState)(implicit context: SimulationContext): Unit = {
    assert(dynamics.isInstanceOf[RemoteDroneDynamics], "Trying to apply state to locally computed drone.")
    dynamics.asInstanceOf[RemoteDroneDynamics].update(state)
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

  def immobile = droneModules.exists(_.exists(_.cancelMovement))

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
    } else if (storedResources == 0) {
      warn("Drone has no minerals to deposit.")
    } else if ((other.position - position).lengthSquared > (radius + other.radius + 12) * (radius + other.radius + 12)) {
      warn("Too far away to deposit minerals.")
    } else {
      for (s <- storage) s.depositResources(other.storage)
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
  def isHarvesting: Boolean = storage.exists(_.isHarvesting)
  def storageCapacity = spec.storageModules
  def size = spec.size
  def radius = spec.radius
  def player = context.player

  def availableStorage: Int = {
    for (s <- storage) yield s.availableStorage
  }.getOrElse(0)

  def storedResources: Int = {
    for (s <- storage) yield s.storedResources
  }.getOrElse(0)

  override def descriptor: Seq[WorldObjectDescriptor] = {
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

  private def moduleDescriptors: Seq[DroneModuleDescriptor] = {
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


import upickle.default._
private[core] sealed trait SerializableDroneCommand
@key("Construct") private[core] case class SerializableConstructDrone(spec: DroneSpec, position: Vector2) extends SerializableDroneCommand
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
  )(implicit context: SimulationContext): DroneCommand = {
    import scala.language.implicitConversions
    implicit def droneLookup(droneID: Int): DroneImpl = {
      if (context.droneRegistry.contains(droneID)) context.droneRegistry(droneID)
      else throw new Exception(f"Cannot find drone with id $droneID. Available IDs: ${context.droneRegistry.keys}")
    }
    implicit def mineralLookup(mineralID: Int): MineralCrystalImpl = {
      if (context.mineralRegistry.contains(mineralID)) context.mineralRegistry(mineralID)
      else throw new Exception(f"Cannot find mineral with id $mineralID. Available IDs: ${context.mineralRegistry.keys}")
    }
    serialized match {
      case SerializableConstructDrone(spec, position) => ConstructDrone(spec, new DummyDroneController, position)
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


import upickle.default._

case class SerializableSpawn(spec: DroneSpec, position: Vector2, playerID: Int, resources: Int, name: Option[String]) {
  def deserialize: Spawn =
    Spawn(spec, position, Player.fromID(playerID), resources, name)
}
object SerializableSpawn {
  def apply(spawn: Spawn): SerializableSpawn =
    SerializableSpawn(spawn.droneSpec, spawn.position, spawn.player.id, spawn.resources, spawn.name)
}

sealed trait MultiplayerMessage

@key("Cmds") case class CommandsMessage(
  commands: Seq[(Int, SerializableDroneCommand)]
) extends MultiplayerMessage
@key("State") case class WorldStateMessage(worldState: Iterable[DroneStateMessage]) extends MultiplayerMessage
@key("Start") case class InitialSync(
  worldSize: Rectangle,
  minerals: Seq[MineralSpawn],
  initialDrones: Seq[SerializableSpawn],
  localPlayerIDs: Set[Int],
  remotePlayerIDs: Set[Int]
) extends MultiplayerMessage {
  def worldMap: WorldMap = new WorldMap(
    minerals,
    worldSize,
    initialDrones.map(_.deserialize),
    None
  )

  def localPlayers: Set[Player] = localPlayerIDs.map(Player.fromID)
  def remotePlayers: Set[Player] = remotePlayerIDs.map(Player.fromID)
}
@key("Register") case object Register extends MultiplayerMessage



object MultiplayerMessage {
  def parse(json: String): MultiplayerMessage =
    read[MultiplayerMessage](json)

  def serialize(commands: Seq[(Int, SerializableDroneCommand)]): String =
    write(CommandsMessage(commands))

  def serialize(worldState: Iterable[DroneStateMessage]): String =
     write[WorldStateMessage](WorldStateMessage(worldState))

  def serialize(
    worldSize: Rectangle,
    minerals: Seq[MineralSpawn],
    initialDrones: Seq[Spawn],
    localPlayers: Set[Player],
    remotePlayers: Set[Player]
  ): String = write(InitialSync(
    worldSize,
    minerals,
    initialDrones.map(x => SerializableSpawn(x)),
    localPlayers.map(_.id),
    remotePlayers.map(_.id)
  ))

  def register: String = write(Register)
}


