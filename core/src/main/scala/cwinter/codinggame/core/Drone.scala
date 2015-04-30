package cwinter.codinggame.core

import cwinter.codinggame.util.maths.{VertexXY, Geometry, Rng, Vector2}
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.worldstate.{BluePlayer, DroneDescriptor, WorldObjectDescriptor}


// TODO: make private[core] once DroneHandle class is implemented
class Drone(
  val modules: Seq[Module],
  val size: Int,
  val controller: DroneController,
  initialPos: Vector2,
  time: Double,
  startingResources: Int = 0
) extends WorldObject {

  // constants for drone construction
  final val ConstructionPeriod = 175
  final val ResourceCost = 5

  val dynamics: DroneDynamics = new DroneDynamics(this, 100, radius, initialPos, time)
  val storageCapacity = modules.count(_ == StorageModule)
  val nLasers = modules.count(_ == Lasers)
  val factoryCapacity = modules.count(_ == NanobotFactory)

  var objectsInSight: Set[WorldObject] = Set.empty[WorldObject]

  private[core] var constructionProgress: Option[Int] = None

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  private[this] var hullState = List.fill[Byte](size - 1)(2)

  private var _storedMinerals = List.empty[MineralCrystal]
  def storedMinerals: List[MineralCrystal] = _storedMinerals
  private def storedMinerals_=(value: List[MineralCrystal]): Unit = _storedMinerals = value

  private[this] var storedEnergyGlobes: Int = startingResources

  private[this] val oldPositions = collection.mutable.Queue.empty[(Float, Float, Float)]
  private final val NJetPositions = 12

  private[this] var movementCommand: MovementCommand = HoldPosition

  private[this] var simulatorEvents = List.empty[SimulatorEvent]

// TODO: ensure canonical module ordering
  private[this] val weapons: Option[DroneLasersModule] = Some(
    new DroneLasersModule(modules.zipWithIndex.filter(_._1 == Lasers).map(_._2), this))
  private[this] val factories: Option[DroneFactoryModule] = Some(
    new DroneFactoryModule(modules.zipWithIndex.filter(_._1 == NanobotFactory).map(_._2), this)
  )


  def initialise(time: Double): Unit = {
    dynamics.setTime(time)
    controller.initialise(this)
  }

  def processEvents(): Unit = {
    movementCommand match {
      case MoveToPosition(position) =>
        if (position ~ this.position) {
          movementCommand = HoldPosition
          enqueueEvent(ArrivedAtPosition)
          dynamics.halt()
        }
      case _ => // don't care
    }

    // process events
    eventQueue foreach {
      case Spawned => controller.onSpawn()
      case MineralEntersSightRadius(mineral) => controller.onMineralEntersVision(mineral)
      case ArrivedAtPosition => controller.onArrival()
      case DroneEntersSightRadius(drone) => controller.onDroneEntersVision(drone)
      case event => throw new Exception(s"Unhandled event! $event")
    }
    eventQueue.clear()
    controller.onTick()
  }

  override def update(): Seq[SimulatorEvent] = {
    movementCommand match {
      case MoveInDirection(direction) =>
        dynamics.orientation = direction.normalized
      case MoveToPosition(position) =>
        val dist = position - this.position
        val speed = 100 / 30 // TODO: improve this
        if (dist ~ Vector2.NullVector) {
          // do nothing
        } else if ((dist dot dist) <= speed * speed) {
          dynamics.limitSpeed(dist.size * 30)
          dynamics.orientation = dist.normalized
          dynamics.limitSpeed(100)
        } else {
          dynamics.orientation = dist.normalized
        }
      case HarvestMineralCrystal(mineral) =>
        harvestResource(mineral)
        movementCommand = HoldPosition
      case DepositMineralCrystals(depositee) =>
        if (depositee.availableStorage >= storedMinerals.map(_.size).sum) {
          depositee.storedMinerals :::= storedMinerals
          storedMinerals = List.empty[MineralCrystal]
          movementCommand = HoldPosition
        }
      case HoldPosition =>
        dynamics.halt()
    }



    for (w <- weapons) {
      val (events, resourceCost) = w.update(storedEnergyGlobes)
      simulatorEvents :::= events.toList
      storedEnergyGlobes -= resourceCost
    }
    for (f <- factories) {
      val (events, resourceCost) = f.update(storedEnergyGlobes)
      simulatorEvents :::= events.toList
      storedEnergyGlobes -= resourceCost
    }


    dynamics.update()

    oldPositions.enqueue((position.x.toFloat, position.y.toFloat, dynamics.orientation.orientation.toFloat))
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

    hullState = damageHull(hullState)

    if (hitpoints == 0) {
      dynamics.remove()
      simulatorEvents ::= DroneKilled(this)
    }
  }

  def hitpoints: Int = hullState.map(_.toInt).sum

  def giveMovementCommand(value: MovementCommand): Unit = movementCommand = value

  def startDroneConstruction(command: ConstructDrone): Unit = {
    for (f <- factories) f.startDroneConstruction(command)
  }

  def startMineralProcessing(mineral: MineralCrystal): Unit = {
    // TODO: check that mineral crystal is in storage
    for (f <- factories) {
      storedMinerals = storedMinerals.filter(_ != mineral)
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
    assert(mineralCrystal.size <= availableStorage, s"Crystal size is ${mineralCrystal.size} and storage is only $availableStorage")
    assert(this.position ~ mineralCrystal.position)
    if (!mineralCrystal.harvested) {
      storedMinerals ::= mineralCrystal
      simulatorEvents ::= MineralCrystalHarvested(mineralCrystal)
      mineralCrystal.harvested = true
    }
  }

  override def position: Vector2 = dynamics.pos

  def availableStorage: Int =
    storageCapacity - storedMinerals.map(_.size).sum - math.ceil(storedEnergyGlobes / 7.0).toInt

  def availableFactories: Int = {
    for (f <- factories) yield f.currentCapacity
  }.getOrElse(0)

  def dronesInSight: Set[Drone] = objectsInSight.filter(_.isInstanceOf[Drone]).map { case d: Drone => d }

  def resourceCost: Int = {
    requiredFactories * ResourceCost
  }

  def requiredFactories: Int = {
    size match {
      case 3 => 2
      case 4 => 4
      case x => throw new Exception(s"Drone of size $x!")
    }
  }

  def buildTime: Int = {
    ConstructionPeriod * (size - 1)
  }

  def resourceDepletionPeriod: Int = {
    // TODO: this is not always accurate bc integer division
    buildTime / resourceCost
  }


  override def descriptor: WorldObjectDescriptor = {
    DroneDescriptor(
      id,
      position.x.toFloat,
      position.y.toFloat,
      dynamics.orientation.orientation.toFloat,
      Seq(),//oldPositions :+ (position.x.toFloat, position.y.toFloat, dynamics.orientation.orientation.toFloat),
      moduleDescriptors,
      hullState,
      size,
      BluePlayer,
      constructionProgress
    )
  }


  private def moduleDescriptors: Seq[cwinter.worldstate.DroneModule] = {
    var result = List.empty[cwinter.worldstate.DroneModule]
    var index = 0
    for (
      l <- modules
      if l == Lasers
    ) {
      result ::= cwinter.worldstate.Lasers(index)
      index += 1
    }

    // TODO: do this properly (+rest of this function) and remove .contents once everything is put into modules
    val factoryContents = {
      for (f <- factories)
        yield f.contents
    }.getOrElse(Seq())

    for (n <- factoryContents) {
      result ::= cwinter.worldstate.ProcessingModule(index until index + n)
      index += n
    }
    for (i <- 0 until availableFactories) {
      result ::= cwinter.worldstate.ProcessingModule(Seq(index))
      index += 1
    }

    var storageSum = 0
    // TODO: HarvestedMineral class (no position)
    for (MineralCrystal(size, pos) <- storedMinerals.sortBy(-_.size)) {
      result ::= cwinter.worldstate.StorageModule(index until index + size, -1)
      index += size
      storageSum += size
    }
    var globesRemaining = storedEnergyGlobes
    for (i <- 0 until storageCapacity - storageSum) {
      val globes = math.min(7, globesRemaining)
      result ::= cwinter.worldstate.StorageModule(Seq(index), globes)
      globesRemaining -= globes
      index += 1
    }
    result
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


sealed trait DroneEvent
case object Spawned extends DroneEvent
case class MineralEntersSightRadius(mineralCrystal: MineralCrystal) extends DroneEvent
case object ArrivedAtPosition extends DroneEvent
case class DroneEntersSightRadius(drone: Drone) extends DroneEvent


sealed trait DroneCommand

sealed trait MovementCommand extends DroneCommand
case class MoveInDirection(direction: Vector2) extends MovementCommand
case class MoveToPosition(position: Vector2) extends MovementCommand
case class HarvestMineralCrystal(mineralCrystal: MineralCrystal) extends MovementCommand
case object HoldPosition extends MovementCommand
case class DepositMineralCrystals(drone: Drone) extends MovementCommand

sealed trait ConstructionCommand extends DroneCommand
case class ConstructDrone(drone: Drone) extends ConstructionCommand


