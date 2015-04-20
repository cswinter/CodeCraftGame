package cwinter.codinggame.core

import cwinter.codinggame.maths.Vector2
import cwinter.codinggame.physics.DynamicObject
import cwinter.graphics.model.Geometry
import cwinter.worldstate.{DroneDescriptor, WorldObjectDescriptor}


class Drone(
  val modules: Seq[Module],
  val size: Int,
  val controller: Any,
  initialPos: Vector2,
  time: Double
) extends WorldObject {

  val dynamics: DroneDynamics =
    new DroneDynamics(50, radius, initialPos, time)


  override def position: Vector2 = dynamics.pos

  override private[core] def descriptor: WorldObjectDescriptor = {
    DroneDescriptor(
      id,
      position.x.toFloat,
      position.y.toFloat,
      0,
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

  def moveInDirection(direction: Vector2): Unit = {
    dynamics.setOrientation(direction)
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

