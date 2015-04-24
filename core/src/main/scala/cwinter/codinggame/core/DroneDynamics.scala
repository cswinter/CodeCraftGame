package cwinter.codinggame.core

import cwinter.codinggame.physics._
import cwinter.codinggame.util.maths.{Rectangle, Vector2, Solve}


class DroneDynamics(
  val maxSpeed: Double,
  val radius: Double,
  initialPosition: Vector2,
  initialTime: Double
) extends DynamicObject[DroneDynamics](initialPosition, initialTime) {
  private var velocity: Vector2 = Vector2.NullVector
  private var _orientation: Vector2 = Vector2(1, 0)
  private var speed = maxSpeed
  private var isStunned: Boolean = false


  protected def computeNewPosition(timeDelta: Double): Vector2 =
    pos + timeDelta * velocity

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

  def halt(): Unit = velocity = Vector2.NullVector

  def orientation: Vector2 = _orientation


  def computeCollisionTime(other: DroneDynamics, timeDelta: Double): Option[Double] = {
    // need to calculate the intersection (if any), of two circles moving at constant speed
    // this is equivalent to a stationary circle with combined radius and a moving point

    // if the two circles are (barely) overlapping, this means they just collided
    // in this case, return no further collision times
    // TODO: make sure that overlap is within bounds of numerical error
    val diff = pos - other.pos
    if ((diff dot diff) <= (this.radius + other.radius) * (this.radius + other.radius)) {
      assert(math.abs(diff dot diff) - (this.radius + other.radius) * (this.radius + other.radius) <= 0.0000001)
      return None
    }
    if (this.velocity == Vector2.NullVector && other.velocity == Vector2.NullVector) {
      return None
    }

    // transform to frame of reference of this object
    val position = other.pos - pos
    val relativeVelocity = other.velocity - velocity
    val radius = this.radius + other.radius

    if (relativeVelocity.x == 0 && relativeVelocity.y == 0) return None

    val a = relativeVelocity dot relativeVelocity
    val b = 2 * relativeVelocity dot position
    val c = (position dot position) - radius * radius

    for {
      t <- Solve.quadratic(a, b, c)
      if t <= timeDelta
    } yield t
  }

  def computeWallCollisionTime(areaBounds: Rectangle, timeDelta: Double): Option[(Double, Direction)] = {
    val ctX =
      if (velocity.x > 0) Some(((areaBounds.xMax - pos.x) / velocity.x, East))
      else if (velocity.x < 0) Some(((areaBounds.xMin - pos.x) / velocity.x, West))
      else None

    val ctY =
      if (velocity.y > 0) Some(((areaBounds.yMax - pos.y) / velocity.y, North))
      else if (velocity.y < 0) Some(((areaBounds.yMin - pos.y) / velocity.y, South))
      else None

    val x = (ctX, ctY) match {
      case (Some((t1, _)), Some((t2, _))) =>
        if (t1 < t2) ctX else ctY
      case (Some(t1), None) => Some(t1)
      case (None, Some(t2)) => Some(t2)
      case (None, None) => None
    }

    x.filter(_._1 < timeDelta)
  }

  def handleObjectCollision(other: DroneDynamics): Unit = {
    val normal = (pos - other.pos).normalized
    velocity = maxSpeed * normal
    other.velocity = -other.maxSpeed * normal
    isStunned = true
    other.isStunned = true
  }

  def handleWallCollision(areaBounds: Rectangle): Unit = {
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

  def update(): Unit = {
    if (isStunned) {
      velocity = 0.95f * velocity
      if (velocity.size <= maxSpeed * 0.1f) {
        isStunned = false
      }
    }
  }

  def unwrap: DroneDynamics = this

  override def toString: String = s"ConstantVelocityObject(pos=$pos, velocity=$velocity)"
}
