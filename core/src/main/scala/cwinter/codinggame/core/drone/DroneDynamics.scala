package cwinter.codinggame.core.drone

import cwinter.codinggame.core.{ConstantVelocityDynamics, MissileDynamics}
import cwinter.codinggame.util.maths.{Rectangle, Vector2}


class DroneDynamics(
  val drone: Drone,
  val maxSpeed: Double,
  val weight: Double,
  radius: Double,
  initialPosition: Vector2,
  initialTime: Double
) extends ConstantVelocityDynamics(radius, initialPosition, initialTime) {
  private var _orientation: Vector2 = Vector2(1, 0)
  private var speed = maxSpeed
  private var isStunned: Boolean = false


  def setPosition(value: Vector2): Unit = pos = value

  def orientation_=(orientation: Vector2): Unit = {
    if (isStunned) return
    this._orientation = orientation.normalized
    velocity = speed * orientation
  }

  def limitSpeed(limit: Double): Unit = {
    assert(limit <= maxSpeed)
    speed = limit
  }

  def halt(): Unit = if (!isStunned) velocity = Vector2.NullVector

  def orientation: Vector2 = _orientation


  override def handleObjectCollision(other: ConstantVelocityDynamics): Unit = {
    other match {
      case other: DroneDynamics =>
        val v1 = velocity
        val v2 = other.velocity
        val x1 = pos
        val x2 = other.pos
        val w1 = weight
        val w2 = other.weight
        velocity = v1 - 2 * w2 / (w1 + w2) * (v1 - v2 dot x1 - x2) / (x1 - x2).magnitudeSquared * (x1 - x2)
        other.velocity = v2 - 2 * w1 / (w1 + w2) * (v2 - v1 dot x2 - x1) / (x2 - x1).magnitudeSquared * (x2 - x1)
        isStunned = true
        other.isStunned = true
      case missile: MissileDynamics =>
        missile.handleObjectCollision(this)
        // should probably do something here...
    }
  }

  override def handleWallCollision(areaBounds: Rectangle): Unit = {
    // find closest wall
    val dx = math.min(math.abs(pos.x + areaBounds.xMax), math.abs(pos.x + areaBounds.xMin))
    val dy = math.min(math.abs(pos.y + areaBounds.yMax), math.abs(pos.y + areaBounds.yMin))
    if (dx < dy) {
      velocity = velocity.copy(x = -velocity.x)
      //orientation = orientation.copy(x = -orientation.x)
    } else {
      velocity = velocity.copy(y = -velocity.y)
      //orientation = orientation.copy(y = -orientation.y)
    }
    isStunned = true
  }

  override def update(): Unit = {
    if (isStunned) {
      velocity = 0.9f * velocity
      if (velocity.size <= maxSpeed * 0.1f) {
        isStunned = false
      }
    }
  }

  override def toString: String = s"DroneDynamics(pos=$pos, velocity=$velocity)"
}
