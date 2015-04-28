package cwinter.codinggame.core

import cwinter.codinggame.util.maths.{Rectangle, Vector2}


class MissileDynamics(
  val speed: Double,
  val target: ConstantVelocityDynamics,
  initialPosition: Vector2,
  initialTime: Double
) extends ConstantVelocityDynamics(1, initialPosition, initialTime) {

  override def handleObjectCollision(other: ConstantVelocityDynamics): Unit = {
    // shouldn't collide with other missiles (or maybe you should!!! i get this for free, so why not?)
    // destroy this object, create event for damage to other object (if it is Drone)
  }

  override def handleWallCollision(areaBounds: Rectangle): Unit = {
    // just die on collision (or maybe bounce?)
  }

  override def update(): Unit = {
    val targetDirection = target.pos - pos
    velocity = speed * targetDirection.normalized
  }
}
