package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core._
import cwinter.codecraft.core.api.{DroneControllerBase, DroneSpec, MineralCrystal}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.objects.{MineralCrystalImpl, EnergyGlobeObject, WorldObject}
import cwinter.codecraft.core.replay._
import cwinter.codecraft.util.maths.{Float0To1, Vector2}
import cwinter.codecraft.worldstate.{DroneDescriptor, DroneModuleDescriptor, Player, WorldObjectDescriptor}


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

  final val MessageCooldown = 20
  private[this] var messageCooldown = MessageCooldown
  private final val NJetPositions = 6
  private[this] val oldPositions = collection.mutable.Queue.empty[(Float, Float, Float)]


  // TODO: remove this once all logic is moved into modules
  private[this] var simulatorEvents = List.empty[SimulatorEvent]

  val dynamics: DroneDynamics = spec.constructDynamics(this, initialPos, time)
  private[this] val weapons = spec.constructMissilesBatteries(this)
  private[this] val factories = spec.constructRefineries(this)
  private[core] val storage = spec.constructStorage(this, startingResources)
  private[this] val manipulator = spec.constructManipulatorModules(this)
  private[this] val shieldGenerators = spec.constructShieldGenerators(this)
  private[this] val engines = spec.constructEngineModules(this)
  val droneModules = Seq(weapons, factories, storage, manipulator, shieldGenerators, engines)


  def initialise(time: Double): Unit = {
    dynamics.setTime(time)
    controller.initialise(this)
  }

  def processEvents(): Unit = {
    for (mineralCrystal <- storedMinerals) {
      if (availableFactories >= mineralCrystal.size) {
        startMineralProcessing(mineralCrystal)
      }
    }

    for (event <- dynamics.arrivalEvent) {
      enqueueEvent(event)
    }

    if (hasDied) {
      controller.onDeath()
    } else {
      // process events
      eventQueue foreach {
        case Spawned => controller.onSpawn()
        case Destroyed => controller.onDeath()
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
        f <- factories
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
      factories match {
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
    for (f <- factories) yield f.currentCapacity
  }.getOrElse(0)

  def availableResources: Int = {
    for (s <- storage) yield s.availableResources
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
        player,
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

  override def hasDied = _hasDied

  override def toString = id.toString
}


sealed trait DroneEvent
case object Spawned extends DroneEvent
case object Destroyed extends DroneEvent
case class MineralEntersSightRadius(mineralCrystal: MineralCrystalImpl) extends DroneEvent
case object ArrivedAtPosition extends DroneEvent
case class ArrivedAtMineral(mineral: MineralCrystalImpl) extends DroneEvent
case class ArrivedAtDrone(drone: DroneImpl) extends DroneEvent
case class DroneEntersSightRadius(drone: DroneImpl) extends DroneEvent


sealed trait DroneCommand
case class ConstructDrone(spec: DroneSpec, controller: DroneControllerBase, position: Vector2) extends DroneCommand
case class ProcessMineral(mineralCrystal: MineralCrystalImpl) extends DroneCommand
case class FireMissiles(target: DroneImpl) extends DroneCommand
case class DepositMinerals(target: DroneImpl) extends DroneCommand
case class HarvestMineral(mineral: MineralCrystalImpl) extends DroneCommand

sealed trait MovementCommand extends DroneCommand
case class MoveInDirection(direction: Double) extends MovementCommand
case class MoveToPosition(position: Vector2) extends MovementCommand
case class MoveToMineralCrystal(mineralCrystal: MineralCrystalImpl) extends MovementCommand
case class MoveToDrone(drone: DroneImpl) extends MovementCommand
case object HoldPosition extends MovementCommand


object DroneCommand {
  final val CaseClassRegex = """(\w*?)\((.*)\)""".r

  def unapply(string: String)
      (implicit droneRegistry: Map[Int, DroneImpl], mineralRegistry: Map[Int, MineralCrystalImpl]): Option[DroneCommand] = string match {
    case CaseClassRegex("ConstructDrone", params) =>
      val p = smartSplit(params)
      val CaseClassRegex("DroneSpec", specParamsStr) = p(0)
      val specParams = specParamsStr.split(",")
      val spec = new DroneSpec(specParams(0).toInt, specParams(1).toInt, specParams(2).toInt, specParams(3).toInt, specParams(4).toInt, specParams(5).toInt)
      val controller = new DummyDroneController
      val position = Vector2(p(2))
      Some(ConstructDrone(spec, controller, position))
    case CaseClassRegex("MoveToPosition", params) =>
      Some(MoveToPosition(Vector2(params)))
    case CaseClassRegex("MoveToDrone", AsInt(id)) =>
      Some(MoveToDrone(droneRegistry(id)))
    case CaseClassRegex("MoveToMineralCrystal", AsInt(id)) =>
      Some(MoveToMineralCrystal(mineralRegistry(id)))
    case CaseClassRegex("HarvestMineral", AsInt(id)) =>
      Some(HarvestMineral(mineralRegistry(id)))
    case CaseClassRegex("DepositMinerals", AsInt(droneID)) =>
      Some(DepositMinerals(droneRegistry(droneID)))
    case CaseClassRegex("FireMissiles", AsInt(targetID)) =>
      Some(FireMissiles(droneRegistry(targetID)))
    case CaseClassRegex("MoveInDirection", AsDouble(direction)) =>
      Some(MoveInDirection(direction))
    case _ => None
  }

  private def smartSplit(string: String): IndexedSeq[String] = {
    val result = new collection.mutable.ArrayBuffer[String]
    var pcount = 0
    var i = 0
    val currString = new StringBuilder
    while (i < string.length) {
      val char = string.charAt(i)
      if (char == '(') {
        pcount += 1
      } else if (char == ')') {
        pcount -= 1
      }

      if (char == ',' && pcount == 0) {
        result += currString.toString
        currString.clear()
      } else {
        currString.append(char)
      }

      i += 1
    }
    result += currString.toString

    result
  }
}


