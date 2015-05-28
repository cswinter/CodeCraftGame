package cwinter.codinggame.core.objects

import cwinter.codinggame.physics._
import cwinter.codinggame.util.maths.{Rectangle, Solve, Vector2}

abstract private[core] class ConstantVelocityDynamics(
  val radius: Double,
  val collisionID: Int,
  val alwaysCollide: Boolean,
  initialPosition: Vector2,
  initialTime: Double
) extends DynamicObject[ConstantVelocityDynamics](initialPosition, initialTime) {
  protected var velocity: Vector2 = Vector2.NullVector


  def update(): Unit
  // inherited form DynamicObject:
  // def handleObjectCollision(other: ConstantVelocityDynamics): Unit
  // def handleWallCollision(area: Rectangle): Unit


  override def unwrap: ConstantVelocityDynamics = this

  override protected def computeNewPosition(timeDelta: Double): Vector2 =
    pos + timeDelta * velocity

  override protected def computeWallCollisionTime(
    areaBounds: Rectangle, timeDelta: Double
  ): Option[(Double, Direction)] = {
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

    val result = x.filter(_._1 < timeDelta).map {
      case (t, dir) =>
        if (t < 0 && t > 1e-12) (t, dir)
        else (t, dir)
    }

    if (result.map(_._1 < 0) == Some(true)) {
      println(s"velocity=$velocity\nareaBounds=$areaBounds\ntimeDelta=$timeDelta\nctX=$ctX\nctY=$ctY\nresult=$result\npos=$pos")
    }

    result
  } ensuring(x => x.map(_._1 < 0) != Some(true))

  override protected def computeCollisionTime(
    other: ConstantVelocityDynamics,
    timeDelta: Double
  ): Option[Double] = {
    if (
      (!alwaysCollide && !other.alwaysCollide) ||
      (collisionID == other.collisionID && (!alwaysCollide || !other.alwaysCollide))
    ) return None
    
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
}


