package cwinter.codinggame.physics

import cwinter.codinggame.maths.{Vector2, Rng}
import robowars.worldstate.{Circle, WorldObject}


class MovingObject[TDynamics](
  val objectDynamics: DynamicObject[TDynamics]
) {
  val id = UID()

  def state: WorldObject = Circle(id, objectDynamics.pos.x.toFloat, objectDynamics.pos.y.toFloat, 50)

  @inline def update(t: Double) =
    objectDynamics.updatePosition(t)
  @inline def wallCollision() =
    objectDynamics.handleWallCollision()
  @inline def collision(other: MovingObject[TDynamics]) =
    objectDynamics.handleObjectCollision(other.objectDynamics.unwrap)
  @inline def collisionTime(other: MovingObject[TDynamics], t: Double) =
    objectDynamics.collisionTime(other.objectDynamics.unwrap, t)
  @inline def wallCollisionTime(t: Double) =
    objectDynamics.wallCollisionTime(t)
}


object MovingObject {
  def apply() = {
    new MovingObject(new ConstantVelocityObject(Rng.vector2(-500, 500, -500, 500), Rng.vector2(200)))
  }

  def apply(position: Vector2) = {
    new MovingObject(new ConstantVelocityObject(position, Rng.vector2(200)))
  }
}


object UID {
  private[this] var _count = -1
  def apply(): Int = {
    _count += 1
    _count
  }
}
