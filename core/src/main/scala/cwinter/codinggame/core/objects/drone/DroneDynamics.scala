package cwinter.codinggame.core.objects.drone

import cwinter.codinggame.core.errors.Errors
import cwinter.codinggame.core.objects.{ConstantVelocityDynamics, MissileDynamics}
import cwinter.codinggame.graphics.engine.Debug
import cwinter.codinggame.util.maths.{ColorRGBA, Rectangle, Vector2}


private[core] class DroneDynamics(
  val drone: Drone,
  val maxSpeed: Double,
  val weight: Double,
  radius: Double,
  initialPosition: Vector2,
  initialTime: Double
) extends ConstantVelocityDynamics(radius, drone.player.id, true, initialPosition, initialTime) {
  final val MaxTurnSpeed = 0.25
  private var _orientation: Double = 0
  private var speed = maxSpeed
  private var isStunned: Boolean = false
  private[this] var _movementCommand: MovementCommand = HoldPosition


  def setMovementCommand(value: MovementCommand): Boolean = {
    value match {
      case MoveToPosition(p) if p ~ pos => return true
      case _ =>
    }
    val redundant = value == _movementCommand
    _movementCommand = value
    redundant
  }

  def orientation_=(value: Double): Unit = _orientation = value
  def orientation: Double = _orientation

  def setPosition(value: Vector2): Unit = pos = value


  def limitSpeed(limit: Double): Unit = {
    assert(limit <= maxSpeed)
    speed = limit
  }

  private def halt(): Unit = {
    assert(!isStunned)
    velocity = Vector2.Null
    _movementCommand = HoldPosition
  }


  def adjustOrientation(target: Double): Unit = {
    if (target == orientation) return

    val orientation2 =
      if (target > orientation) orientation + 2 * math.Pi
      else orientation

    val diff = orientation2 - target

    if (diff <= MaxTurnSpeed || diff >= 2 * math.Pi - MaxTurnSpeed) {
      orientation = target
    } else if (diff > math.Pi) {
      orientation += MaxTurnSpeed
    } else {
      orientation -= MaxTurnSpeed
    }

    if (orientation < 0) orientation += 2 * math.Pi
    if (orientation > 2 * math.Pi) orientation -= 2 * math.Pi
  }

  def hasArrived: Boolean = _movementCommand match {
    case MoveToPosition(position) if position ~ this.pos =>
      halt()
      true
    case _ => false
  }


  override def update(): Unit = {
    velocity =
      if (isStunned) {
        if (velocity.size <= maxSpeed * 0.05f) {
          isStunned = false
          Vector2.Null
        } else velocity * 0.9f
      } else {
        _movementCommand match {
          case MoveInDirection(direction) =>
            val targetOrientation = direction.orientation
            adjustOrientation(targetOrientation)

            if (targetOrientation == orientation) {
              maxSpeed * Vector2(orientation)
            } else {
              Vector2.Null
            }
          case MoveToPosition(position) =>
            val dist = position - this.pos

            if (dist ~ Vector2.Null) {
              Vector2.Null
            } else {
              val targetOrientation = dist.orientation
              adjustOrientation(targetOrientation)
              if (targetOrientation == orientation) {
                val speed = maxSpeed / 30 // TODO: improve this
                if ((dist dot dist) > speed * speed) {
                  maxSpeed * dist.normalized
                } else {
                  val distance = dist.size
                  distance * 30 * dist.normalized
                }
              } else {
                Vector2.Null
              }
            }
          case HoldPosition =>
            Vector2.Null
        }
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

  override def toString: String = s"DroneDynamics(pos=$pos, velocity=$velocity)"
}
