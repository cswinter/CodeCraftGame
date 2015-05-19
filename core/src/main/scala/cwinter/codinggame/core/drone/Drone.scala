package cwinter.codinggame.core.drone

import cwinter.codinggame.core._
import cwinter.codinggame.util.maths.{Float0To1, Vector2}
import cwinter.codinggame.worldstate.{DroneDescriptor, DroneModuleDescriptor, Player, WorldObjectDescriptor}


// TODO: make private[core] once DroneHandle class is implemented
class Drone(
  val spec: DroneSpec,
  val controller: DroneController,
  val player: Player,
  initialPos: Vector2,
  time: Double,
  startingResources: Int = 0
) extends WorldObject {


  var objectsInSight: Set[WorldObject] = Set.empty[WorldObject]

  private[core] var constructionProgress: Option[Int] = None

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  private[this] var hullState = List.fill[Byte](spec.size - 1)(2)

  private[this] val oldPositions = collection.mutable.Queue.empty[(Float, Float, Float)]
  private final val NJetPositions = 6


  private[this] var mineralDepositee: Option[Drone] = None

  private[this] var automaticMineralProcessing: Boolean = true

  // TODO: remove this once all logic is moved into modules
  private[this] var simulatorEvents = List.empty[SimulatorEvent]

  val dynamics: DroneDynamics = spec.constructDynamics(this, initialPos, time)
  private[this] val weapons = spec.constructMissilesBatteries(this)
  private[this] val factories = spec.constructProcessingModules(this)
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

    if (dynamics.hasArrived) {
      enqueueEvent(ArrivedAtPosition)
    }

    if (hasDied) {
      controller.onDeath()
    } else {
      // process events
      eventQueue foreach {
        case Spawned => controller.onSpawn()
        case Destroyed => controller.onDeath()
        case MineralEntersSightRadius(mineral) => controller.onMineralEntersVision(mineral)
        case ArrivedAtPosition => controller.onArrival()
        case DroneEntersSightRadius(drone) => controller.onDroneEntersVision(drone)
        case event => throw new Exception(s"Unhandled event! $event")
      }
      eventQueue.clear()
      controller.onTick()
    }
  }

  def depositMineral(crystal: MineralCrystal, pos: Vector2): Unit = {
    for {
      s <- storage
    } s.depositMineral(crystal, pos)
  }

  override def update(): Seq[SimulatorEvent] = {
    for {
      depositee <- mineralDepositee
      capacity = depositee.availableStorage
      s <- storage
      (min, pos) <- s.popMineralCrystal(capacity)
    } {
      depositee.depositMineral(min, pos)
      if (s.storedMinerals.isEmpty) {
        mineralDepositee = None
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

  def hitpoints: Int = hullState.map(_.toInt).sum

  def giveMovementCommand(value: MovementCommand): Unit = {
    if (droneModules.exists(_.exists(_.cancelMovement))) {
      // TODO: warning/error message? queue up command?
      return
    }
    dynamics.movementCommand_=(value)
  }

  def startDroneConstruction(command: ConstructDrone): Unit = {
    for (m <- manipulator) m.startDroneConstruction(command)
  }

  def startMineralProcessing(mineral: MineralCrystal): Unit = {
    // TODO: check that mineral crystal is in storage
    for (f <- factories) {
      storage.get.removeMineralCrystal(mineral)
      f.startMineralProcessing(mineral)
    }
  }

  def fireWeapons(target: Drone): Unit = {
    for (w <- weapons) w.fire(target)
  }

  def weaponsCooldown: Int = weapons.map(_.cooldown).getOrElse(1)

  def harvestResource(mineralCrystal: MineralCrystal): Unit = {
    // TODO: better error messages, add option to emit warnings and abort instead of throwing
    // TODO: harvesting takes some time to complete
    // TODO: make this part of mineral module
    for (s <- storage) s.harvestMineral(mineralCrystal)
  }

  override def position: Vector2 = dynamics.pos


  def availableStorage: Int = {
    for (s <- storage) yield s.availableStorage
  }.getOrElse(0)

  def availableFactories: Int = {
    for (f <- factories) yield f.currentCapacity
  }.getOrElse(0)

  def availableResources: Int = {
    for (s <- storage) yield s.availableResources
  }.getOrElse(0)

  def storedMinerals: Iterable[MineralCrystal] = {
    for (s <- storage) yield s.storedMinerals
  }.getOrElse(Seq())

  def depositMinerals(other: Drone): Unit = {
    assert(other != this)

    mineralDepositee = Some(other)
  }

  def dronesInSight: Set[Drone] = objectsInSight.filter(_.isInstanceOf[Drone]).map { case d: Drone => d }

  def isConstructing: Boolean = manipulator.map(_.isConstructing) == Some(true)

  def storageCapacity = spec.storageModules
  def processingCapacity = spec.processingModules
  def size = spec.size
  def radius = spec.radius


  override def descriptor: Seq[WorldObjectDescriptor] = {
    Seq(
    DroneDescriptor(
      id,
      position.x.toFloat,
      position.y.toFloat,
      dynamics.orientation.toFloat,
      Seq(),//oldPositions :+ (position.x.toFloat, position.y.toFloat, dynamics.orientation.toFloat),
      moduleDescriptors,
      hullState,
      shieldGenerators.map(_.hitpointPercentage),
      spec.size,
      player,
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


  override def hasDied = false
}


sealed trait DroneEvent
case object Spawned extends DroneEvent
case object Destroyed extends DroneEvent
case class MineralEntersSightRadius(mineralCrystal: MineralCrystal) extends DroneEvent
case object ArrivedAtPosition extends DroneEvent
case class DroneEntersSightRadius(drone: Drone) extends DroneEvent


sealed trait DroneCommand

sealed trait MovementCommand extends DroneCommand
case class MoveInDirection(direction: Vector2) extends MovementCommand
case class MoveToPosition(position: Vector2) extends MovementCommand
case object HoldPosition extends MovementCommand

sealed trait ConstructionCommand extends DroneCommand
case class ConstructDrone(drone: Drone) extends ConstructionCommand


