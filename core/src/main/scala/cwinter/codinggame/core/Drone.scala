package cwinter.codinggame.core

import cwinter.codinggame.maths.Vector2
import cwinter.graphics.model.Geometry
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

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)

  private[this] var storedMinerals = List.empty[MineralCrystal]
  private[this] var storedEnergyGlobes: Int = startingResources

  private[this] var _command: Option[DroneCommand] = None


  def processEvents(): Unit = {
    _command match {
      case Some(MoveToPosition(position)) =>
        if (position ~ this.position) {
          _command = None
          enqueueEvent(ArrivedAtPosition)
          dynamics.halt()
        }
      case _ => // don't care
    }

    eventQueue foreach {
      case Spawned => controller.onSpawn()
      case MineralEntersSightRadius(mineral) => controller.onMineralEntersVision(mineral)
      case ArrivedAtPosition => controller.onArrival()
      case event => throw new Exception(s"Unhandled event! $event")
    }
    eventQueue.clear()
    controller.onTick()
  }

  def processCommands(): Unit = {
    _command match {
      case Some(MoveInDirection(direction)) =>
        dynamics.orientation = direction.normalized
        _command = None
      case Some(MoveToPosition(position)) =>
        val dist = position - this.position
        val speed = 100 / 30 // TODO: improve this
        if ((dist dot dist) <= speed * speed) {
          dynamics.limitSpeed(dist.size * 30)
          dynamics.orientation = dist.normalized
          dynamics.limitSpeed(100)
        } else {
          dynamics.orientation = dist.normalized
        }
      case Some(HarvestMineralCrystal(mineral)) =>
        harvestResource(mineral)
        _command = None
      case None => // do nothing
    }
  }

  def enqueueEvent(event: DroneEvent): Unit = {
    eventQueue.enqueue(event)
  }


  def command_=(value: DroneCommand): Unit = _command = Some(value)

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


  override def descriptor: WorldObjectDescriptor = {
    DroneDescriptor(
      id,
      position.x.toFloat,
      position.y.toFloat,
      dynamics.orientation.orientation.toFloat,
      Seq(),
      moduleDescriptors,
      Seq.fill[Byte](size - 1)(2),
      size
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

    for (
      f <- modules
      if f == NanobotFactory
    ) {
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

sealed trait DroneCommand

case class MoveInDirection(direction: Vector2) extends DroneCommand
case class MoveToPosition(position: Vector2) extends DroneCommand
case class HarvestMineralCrystal(mineralCrystal: MineralCrystal) extends DroneCommand


