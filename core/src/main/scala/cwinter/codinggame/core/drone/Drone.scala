package cwinter.codinggame.core.drone

import cwinter.codinggame.core._
import cwinter.codinggame.util.maths.{Geometry, Vector2}
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.worldstate.{DroneDescriptor, Player, WorldObjectDescriptor}


// TODO: make private[core] once DroneHandle class is implemented
class Drone(
  val modules: Seq[Module],
  val size: Int,
  val controller: DroneController,
  val player: Player,
  initialPos: Vector2,
  time: Double,
  startingResources: Int = 0,
  val isMothership: Boolean = false
) extends WorldObject {

  // constants for drone construction
  final val ConstructionPeriod = 50
  final val ResourceCost = 2

  val dynamics: DroneDynamics = new DroneDynamics(this, maximumSpeed, weight, radius, initialPos, time)
  val storageCapacity = modules.count(_ == StorageModule)
  val nLasers = modules.count(_ == Lasers)
  val factoryCapacity = modules.count(_ == NanobotFactory)

  var objectsInSight: Set[WorldObject] = Set.empty[WorldObject]

  private[core] var constructionProgress: Option[Int] = None

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  private[this] var hullState = List.fill[Byte](size - 1)(2)

  private[this] val oldPositions = collection.mutable.Queue.empty[(Float, Float, Float)]
  private final val NJetPositions = 12


  // TODO: remove this once all logic is moved into modules
  private[this] var simulatorEvents = List.empty[SimulatorEvent]

  // TODO: unify module creation, make sure to assign None to nonexisting modules
  // TODO: ensure canonical module ordering
  private[this] val weapons: Option[DroneLasersModule] = Some(
    new DroneLasersModule(modules.zipWithIndex.filter(_._1 == Lasers).map(_._2), this))
  private[this] val factories: Option[DroneFactoryModule] = Some(
    new DroneFactoryModule(modules.zipWithIndex.filter(_._1 == NanobotFactory).map(_._2), this)
  )// TODO: change to private[this] once storage module is implemented properly
  private[core] val storage: Option[DroneStorageModule] = Some(
    new DroneStorageModule(modules.zipWithIndex.filter(_._1 == StorageModule).map(_._2), this, startingResources)
  )
  private[this] val manipulator: Option[DroneManipulatorModule] = Some(
    new DroneManipulatorModule(modules.zipWithIndex.filter(_._1 == Manipulator).map(_._2), this)
  )
  private[this] val shieldGenerators: Option[DroneShieldGeneratorModule] = Some(
    new DroneShieldGeneratorModule(modules.zipWithIndex.filter(_._1 == ShieldGenerator).map(_._2), this)
  )
  private[this] val engines: Option[DroneEnginesModule] = Some(
    new DroneEnginesModule(modules.zipWithIndex.filter(_._1 == Engines).map(_._2), this)
  )
  val droneModules = Seq(weapons, factories, storage, manipulator, shieldGenerators, engines)


  def initialise(time: Double): Unit = {
    dynamics.setTime(time)
    controller.initialise(this)
  }

  def processEvents(): Unit = {
    if (dynamics.hasArrived) {
      enqueueEvent(ArrivedAtPosition)
    }

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

  override def update(): Seq[SimulatorEvent] = {
    for (Some(m) <- droneModules) {
      val (events, resourceCost) = m.update(availableResources)
      simulatorEvents :::= events.toList
      for (s <- storage) s.modifyResources(resourceCost)
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


    val damage = shieldGenerators.map(_.absorbDamage(1)).getOrElse(0)

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
    for (s <- storage) s.depositMinerals(other.storage)
  }

  def dronesInSight: Set[Drone] = objectsInSight.filter(_.isInstanceOf[Drone]).map { case d: Drone => d }

  def isConstructing: Boolean = manipulator.map(_.isConstructing) == Some(true)

  def resourceCost: Int = {
    ModulePosition.moduleCount(size) * ResourceCost
  }

  def requiredFactories: Int = {
    ModulePosition.moduleCount(size) * 2
  }

  def weight =
    if (isMothership) 10000
    else size + modules.length

  def maximumSpeed: Double = {
    val propulsion = 1 + modules.count(_ == Engines)
    1000 * propulsion / weight
  }

  def buildTime: Int = {
    ConstructionPeriod * (size - 1)
  }

  def resourceDepletionPeriod: Int = {
    // TODO: this is not always accurate bc integer division
    buildTime / resourceCost
  }


  override def descriptor: Seq[WorldObjectDescriptor] = {
    Seq(
    DroneDescriptor(
      id,
      position.x.toFloat,
      position.y.toFloat,
      dynamics.orientation.toFloat,
      Seq(),//oldPositions :+ (position.x.toFloat, position.y.toFloat, dynamics.orientation.orientation.toFloat),
      moduleDescriptors,
      hullState,
      shieldGenerators.map(_.hitpointPercentage),
      size,
      player,
      constructionProgress
    )) ++ manipulator.toSeq.flatMap(_.manipulatorGraphics)
  }

  private def moduleDescriptors: Seq[cwinter.worldstate.DroneModule] = {
    for {
      Some(m) <- droneModules
      descr <- m.descriptors
    } yield descr
  }


  override def hasDied = false


  def radius: Double = {
    val sideLength = 40
    val radiusBody = 0.5f * sideLength / math.sin(math.Pi / size).toFloat
    radiusBody + 0.5f * Geometry.circumradius(4, size)
  }
}


sealed trait Module
case object StorageModule extends Module
case object Lasers extends Module
case object NanobotFactory extends Module
case object Engines extends Module
case object Manipulator extends Module
case object ShieldGenerator extends Module


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


