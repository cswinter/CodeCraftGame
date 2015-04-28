package cwinter.codinggame.core

import cwinter.codinggame.util.maths.{Geometry, Rng, Vector2}
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.worldstate.{BluePlayer, DroneDescriptor, WorldObjectDescriptor}


private[core] class Drone(
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

  val dynamics: DroneDynamics = new DroneDynamics(100, radius, initialPos, time)
  val storageCapacity = modules.count(_ == StorageModule)
  val nLasers = modules.count(_ == Lasers)
  val factoryCapacity = modules.count(_ == NanobotFactory)

  private var constructionProgress: Option[Int] = None
  private var weaponsCooldown: Int = 0

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  private var _storedMinerals = List.empty[MineralCrystal]
  def storedMinerals: List[MineralCrystal] = _storedMinerals
  private def storedMinerals_=(value: List[MineralCrystal]): Unit = _storedMinerals = value

  private[this] var storedEnergyGlobes: Int = startingResources

  private[this] var movementCommand: MovementCommand = HoldPosition
  private[this] var droneConstructions = List.empty[(ConstructDrone, Int)]
  private[this] var mineralProcessing = List.empty[(MineralCrystal, Int)]

  private[this] var simulatorEvents = List.empty[SimulatorEvent]

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

    eventQueue foreach {
      case Spawned => controller.onSpawn()
      case MineralEntersSightRadius(mineral) => controller.onMineralEntersVision(mineral)
      case ArrivedAtPosition => controller.onArrival()
      case DroneEntersSightRadius(drone) => // TODO: implement
      case event => throw new Exception(s"Unhandled event! $event")
    }
    eventQueue.clear()
    controller.onTick()
  }

  def processCommands(): Seq[SimulatorEvent] = {
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
        // TODO: check storage etc
        depositee.storedMinerals :::= storedMinerals
        storedMinerals = List.empty[MineralCrystal]
      case HoldPosition =>
        dynamics.halt()
    }

    var index = 0
    droneConstructions =
      for ((drone, progress) <- droneConstructions)
        yield {
          val requiredFactories = drone.drone.requiredFactories
          drone.drone.dynamics.orientation = dynamics.orientation
          drone.drone.constructionProgress = Some(progress)
          val positions = index until index + requiredFactories
          index += requiredFactories
          val moduleOffset = ModulePosition.center(size, positions)
          val rotation = dynamics.orientation.orientation
          val moduleOffsetVector2 = Vector2(moduleOffset.x, moduleOffset.y).rotated(rotation)
          drone.drone.dynamics.setPosition(position + moduleOffsetVector2)
          if (progress == drone.drone.buildTime) {
            simulatorEvents ::= SpawnDrone(drone.drone)
            drone.drone.constructionProgress = None
            drone.drone.dynamics.setPosition(position - 150 * Rng.vector2())
          }
          if (progress % drone.drone.resourceDepletionPeriod == 0) {
            if (storedEnergyGlobes > 0) {
              storedEnergyGlobes -= 1
              (drone, progress + 1)
            } else {
              (drone, progress)
            }
          } else {
            (drone, progress + 1)
          }
        }

    mineralProcessing =
      for ((mineral, progress) <- mineralProcessing)
        yield {
          val positions = index until index + mineral.size
          val moduleOffset = ModulePosition.center(size, positions)
          index += mineral.size
          mineral.position = position + Vector2(moduleOffset.x, moduleOffset.y)

          if (progress % 25 == 0) {
            storedEnergyGlobes += 1
          }
          (mineral, progress - 1)
        }

    droneConstructions = droneConstructions.filter {
      case (drone, progress) =>
        drone.drone.buildTime >= progress
    }

    mineralProcessing = mineralProcessing.filter {
      case (mineral, remaining) =>
        if (remaining <= 0) simulatorEvents ::= MineralCrystalDestroyed(mineral)
        remaining > 0
    }

    dynamics.update()

    val events = simulatorEvents
    simulatorEvents = List.empty[SimulatorEvent]
    events
  }

  def enqueueEvent(event: DroneEvent): Unit = {
    eventQueue.enqueue(event)
  }


  def giveMovementCommand(value: MovementCommand): Unit = movementCommand = value

  def startDroneConstruction(command: ConstructDrone): Unit = {
    droneConstructions ::= ((command, 0))
    droneConstructions = droneConstructions.sortBy { case (c, p) => c.drone.requiredFactories }
    simulatorEvents ::= DroneConstructionStarted(command.drone)
    command.drone.dynamics.orientation = dynamics.orientation
  }

  def startMineralProcessing(mineral: MineralCrystal): Unit = {
    mineralProcessing ::= (mineral, mineral.size * 7 * 25)
    storedMinerals = storedMinerals.filter(_ != mineral)
    simulatorEvents ::= MineralCrystalActivated(mineral)
    mineral.harvested = true
  }

  def fireWeapons(target: Drone): Unit = {
    if (weaponsCooldown <= 0) {
      weaponsCooldown = 100
      for (i <- modules.filter(_ == Lasers).indices) {
        val offset = ModulePosition(size, i)

      }
    }
  }

  def harvestResource(mineralCrystal: MineralCrystal): Unit = {
    // TODO: better error messages, add option to emit warnings and abort instead of throwing
    // TODO: harvesting takes some time to complete
    assert(mineralCrystal.size <= availableStorage, s"Crystal size is ${mineralCrystal.size} and storage is only $availableStorage")
    assert(this.position ~ mineralCrystal.position)
    storedMinerals ::= mineralCrystal
    simulatorEvents ::= MineralCrystalHarvested(mineralCrystal)
  }

  override def position: Vector2 = dynamics.pos

  def availableStorage: Int =
    storageCapacity - storedMinerals.map(_.size).sum - math.ceil(storedEnergyGlobes / 7.0).toInt

  def availableFactories: Int =
    factoryCapacity -
      droneConstructions.map(d => d._1.drone.requiredFactories).sum -
      mineralProcessing.map(d => d._1.size).sum

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
      Seq(),
      moduleDescriptors,
      Seq.fill[Byte](size - 1)(2),
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

    val factoryContents = (
      droneConstructions.map(_._1.drone.requiredFactories) ++
      mineralProcessing.map(_._1.size)
      ).sorted.reverse

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


