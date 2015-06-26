package cwinter.codecraft.physics

import cwinter.codecraft.util.maths.{Rectangle, Vector2, Solve}


abstract class DynamicObject[T](initialPos: Vector2, initialTime: Double) {
  private[this] var _pos: Vector2 = initialPos
  private[this] var _time: Double = initialTime
  private[this] var _removed: Boolean = false
  def removed: Boolean = _removed
  def remove(): Unit = _removed = true

  def pos: Vector2 = _pos
  protected def pos_=(value: Vector2): Unit = _pos = value


  def setTime(time: Double): Unit = _time = time

  def collisionTime(other: T, time: Double): Option[Double] =
    computeCollisionTime(other, time - _time)

  def wallCollisionTime(areaBounds: Rectangle, time: Double): Option[(Double, Direction)] =
    computeWallCollisionTime(areaBounds, time - _time)

  @inline final def updatePosition(time: Double): Unit = {
    if (time != _time) {
      val dt = time - _time
      if (dt <= 0) throw new Exception(s"dt=$dt") //assert(dt > 0, s"dt=$dt")
      _pos = computeNewPosition(dt)
      _time = time
    }
  }

  def handleWallCollision(areaBounds: Rectangle)
  def handleObjectCollision(other: T)

  def unwrap: T

  protected def computeNewPosition(timeDelta: Double): Vector2
  protected def computeCollisionTime(other: T, timeDelta: Double): Option[Double]
  protected def computeWallCollisionTime(areaBounds: Rectangle, timeDelta: Double): Option[(Double, Direction)]
  // def calculateBoundingBox(pos: Vector2, command: Command, timestep: Float): Vector2
}


class ConstantVelocityObject(
  initialPos: Vector2,
  initialVelocity: Vector2,
  val weight: Double,
  val radius: Double,
  initialTime: Double = 0
) extends DynamicObject[ConstantVelocityObject](initialPos, initialTime) {
  private var velocity: Vector2 = initialVelocity


  protected def computeNewPosition(timeDelta: Double): Vector2 =
    pos + timeDelta * velocity

  def computeCollisionTime(other: ConstantVelocityObject, timeDelta: Double): Option[Double] = {
    // need to calculate the intersection (if any), of two circles moving at constant speed
    // this is equivalent to a stationary circle with combined radius and a moving point

    // if the two circles are (barely) overlapping, this means they just collided
    // in this case, return no further collision times
    // TODO: make sure that overlap is within bounds of numerical error
    val diff = pos - other.pos
    if ((diff dot diff) <= (this.radius + other.radius) * (this.radius + other.radius)) {
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

  def handleObjectCollision(other: ConstantVelocityObject): Unit = {
    val v1 = velocity
    val v2 = other.velocity
    val x1 = pos
    val x2 = other.pos
    val w1 = weight
    val w2 = other.weight
    velocity = v1 - 2 * w2 / (w1 + w2) * (v1 - v2 dot x1 - x2) / (x1 - x2).lengthSquared * (x1 - x2)
    other.velocity = v2 - 2 * w1 / (w1 + w2) * (v2 - v1 dot x2 - x1) / (x2 - x1).lengthSquared * (x2 - x1)
  }

  def handleWallCollision(areaBounds: Rectangle): Unit = {
    // find closest wall
    val dx = math.min(math.abs(pos.x + areaBounds.xMax), math.abs(pos.x + areaBounds.xMin))
    val dy = math.min(math.abs(pos.y + areaBounds.yMax), math.abs(pos.y + areaBounds.yMin))
    if (dx < dy) {
      velocity = velocity.copy(x = -velocity.x)
    } else {
      velocity = velocity.copy(y = -velocity.y)
    }
  }

  def unwrap: ConstantVelocityObject = this

  override def toString: String = s"ConstantVelocityObject(pos=$pos, velocity=$velocity)"
}


sealed trait Direction {
  def x: Int
  def y: Int
  def xAxisAligned: Boolean
}

case object North extends Direction {
  def x = 0
  def y = 1
  def xAxisAligned = false
}

case object South extends Direction {
  def x = 0
  def y = -1
  def xAxisAligned = false
}

case object East extends Direction {
  def x = 1
  def y = 0
  def xAxisAligned = true
}

case object West extends Direction {
  def x = -1
  def y = 0
  def xAxisAligned = true
}