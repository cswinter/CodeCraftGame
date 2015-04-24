package cwinter.codinggame.core

import cwinter.codinggame.util.maths.{Geometry, Rng, Vector2}
import cwinter.worldstate.{DroneDescriptor, WorldObjectDescriptor}


private[core] class Drone(
  val modules: Seq[Module],
  val size: Int,
  val controller: DroneController,
  initialPos: Vector2,
  time: Double,
  startingResources: Int = 0
) extends WorldObject {

  val dynamics: DroneDynamics = new DroneDynamics(100, radius, initialPos, time)
  val storageCapacity = modules.count(_ == StorageModule)
  val nLasers = modules.count(_ == Lasers)
  val factoryCapacity = modules.count(_ == NanobotFactory)

  private var constructionProgress: Option[Int] = None

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  private[this] var storedMinerals = List.empty[MineralCrystal]
  private[this] var storedEnergyGlobes: Int = startingResources

  private[this] var movementCommand: MovementCommand = HoldPosition
  private[this] var droneConstructions = List.empty[(ConstructDrone, Int)]

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
        if ((dist dot dist) <= speed * speed) {
          dynamics.limitSpeed(dist.size * 30)
          dynamics.orientation = dist.normalized
          dynamics.limitSpeed(100)
        } else {
          dynamics.orientation = dist.normalized
        }
      case HarvestMineralCrystal(mineral) =>
        harvestResource(mineral)
        movementCommand = HoldPosition
      case HoldPosition =>
        dynamics.halt()
    }

    droneConstructions =
      for ((drone, progress) <- droneConstructions)
        yield {
          drone.drone.dynamics.orientation = dynamics.orientation
          drone.drone.constructionProgress = Some(progress)
          drone.drone.dynamics.setPosition(position + 27 * Vector2(dynamics.orientation.orientation - 2.2))
          if (progress == 500) {
            simulatorEvents ::= SpawnDrone(drone.drone)
            drone.drone.constructionProgress = None
            drone.drone.dynamics.setPosition(position - 150 * Rng.vector2())
          }
          // TODO: set position (need to know factory offset)
          (drone, progress + 1)
        }

    droneConstructions = droneConstructions.filter(_._2 <= 500)

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
    simulatorEvents ::= DroneConstructionStarted(command.drone)
    command.drone.dynamics.orientation = dynamics.orientation
  }

  def harvestResource(mineralCrystal: MineralCrystal): Unit = {
    // TODO: better error messages, add option to emit warnings and abort instead of throwing
    // TODO: harvesting takes some time to complete
    assert(mineralCrystal.size <= availableStorage, s"Crystal size is ${mineralCrystal.size} and storage is only $availableStorage")
    assert(this.position ~ mineralCrystal.position)
    storedMinerals ::= mineralCrystal
  }

  override def position: Vector2 = dynamics.pos

  def availableStorage: Int =
    storageCapacity - storedMinerals.map(_.size).sum - math.ceil(storedEnergyGlobes / 7.0).toInt

  def availableFactories: Int =
    factoryCapacity - droneConstructions.map(_._1.drone.size - 2).sum


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

    for ((ConstructDrone(drone), _) <- droneConstructions) {
      result ::= cwinter.worldstate.ProcessingModule(index until index + (drone.size - 2))
      index += drone.size - 2
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


  private def radius: Double = {
    val sideLength = 40
    val radiusBody = 0.5f * sideLength / math.sin(math.Pi / size).toFloat
    radiusBody + Geometry.circumradius(4, size)
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

sealed trait ConstructionCommand extends DroneCommand
case class ConstructDrone(drone: Drone) extends ConstructionCommand


