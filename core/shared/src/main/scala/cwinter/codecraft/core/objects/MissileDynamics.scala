package cwinter.codecraft.core.objects

import cwinter.codecraft.core.objects.drone.{ComputedDroneDynamics, DroneDynamics}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}

private[core] class MissileDynamics(
  var speed: Double,
  val target: DroneDynamics,
  val ownerID: Int,
  val missile: HomingMissile,
  initialPosition: Vector2,
  initialTime: Double,
  val acceleration: Double = 0.0
) extends ConstantVelocityDynamics(1, ownerID, false, initialPosition, initialTime) {
  var hasHit = false

  override def handleObjectCollision(other: ConstantVelocityDynamics): Unit = {
    this.remove()

    hasHit = true
    other match {
      case otherMissile: MissileDynamics => otherMissile.remove()
      case otherDrone: ComputedDroneDynamics => otherDrone.drone.missileHit(missile)
    }
  }

  override def handleWallCollision(areaBounds: Rectangle): Unit = {
    this.remove()
    // just die on collision (or maybe bounce?)
  }

  def recomputeVelocity(): Unit = {
    speed += acceleration
    val targetDirection = target.pos - pos
    if (!target.removed && targetDirection.length >= 0.0001) {
      velocity = speed * targetDirection.normalized
    }
  }
}
