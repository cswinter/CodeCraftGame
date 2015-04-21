package cwinter.codinggame.core

import cwinter.codinggame.maths.Vector2
import cwinter.codinggame.physics.DynamicObject
import cwinter.graphics.model.Geometry
import cwinter.worldstate.{DroneDescriptor, WorldObjectDescriptor}


private[core] class Drone(
  val modules: Seq[Module],
  val size: Int,
  val controller: DroneController,
  initialPos: Vector2,
  time: Double
) extends WorldObject {

  val dynamics: DroneDynamics =
    new DroneDynamics(100, radius, initialPos, time)

  private[this] val eventQueue = collection.mutable.Queue[DroneEvent](Spawned)


  def processEvents(): Unit = {
    eventQueue foreach {
      case Spawned => controller.onSpawn()
      case MineralEntersSightRadius(mineral) => controller.onMineralEntersVision(mineral)
      case event => throw new Exception(s"Unhandled event! $event")
    }
    eventQueue.clear()
    controller.onTick()
  }

  def enqueueEvent(event: DroneEvent): Unit = {
    eventQueue.enqueue(event)
  }

  def moveInDirection(direction: Vector2): Unit = {
    dynamics.orientation = direction.normalized
  }

  override def position: Vector2 = dynamics.pos

  override def descriptor: WorldObjectDescriptor = {
    DroneDescriptor(
      id,
      position.x.toFloat,
      position.y.toFloat,
      dynamics.orientation.orientation,
      Seq(),
      modules.zipWithIndex.map {
        case (StorageModule, i) => cwinter.worldstate.StorageModule(Seq(i), 0)
        case (Lasers, i) => cwinter.worldstate.Lasers(i)
      },
      Seq.fill[Byte](size - 1)(2),
      size,
      constructionState = -1
    )
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


sealed trait DroneEvent

case object Spawned extends DroneEvent
case class MineralEntersSightRadius(mineralCrystal: MineralCrystal) extends DroneEvent


