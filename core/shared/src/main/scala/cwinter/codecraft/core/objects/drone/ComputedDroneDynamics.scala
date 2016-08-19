package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.GameConstants.HarvestingRange
import cwinter.codecraft.core.objects.{ConstantVelocityDynamics, MissileDynamics}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}


private[core] class ComputedDroneDynamics(
  val drone: DroneImpl,
  val maxSpeed: Float,
  val weight: Float,
  radius: Float,
  initialPosition: Vector2,
  initialTime: Double
) extends ConstantVelocityDynamics(
  radius,
  drone.player.id,
  true,
  initialPosition,
  initialTime
) with DroneDynamicsBasics {

  final val MaxTurnSpeed = 0.25f
  private var speed = maxSpeed
  private[drone] var isStunned: Boolean = false
  private var oldPos = pos
  private var oldOrientation = orientation


  def setPosition(value: Vector2): Unit = pos = value


  def limitSpeed(limit: Float): Unit = {
    require(limit <= maxSpeed)
    speed = limit
  }

  protected def halt(): Unit = {
    if (!isStunned) velocity = Vector2.Null
    _movementCommand = HoldPosition
  }


  def adjustOrientation(target: Float): Unit = {
    if (target == orientation) return

    val orientation2 =
      if (target > orientation) orientation + 2 * math.Pi
      else orientation

    val diff = orientation2 - target

    _orientation =
      if (diff <= MaxTurnSpeed || diff >= 2 * math.Pi - MaxTurnSpeed) target
      else if (diff > math.Pi) orientation + MaxTurnSpeed
      else orientation - MaxTurnSpeed

    if (orientation < 0) _orientation += (2 * math.Pi).toFloat
    if (orientation > 2 * math.Pi) _orientation -= (2 * math.Pi).toFloat
  }

  override def arrivalEvent: Option[DroneEvent] = _movementCommand match {
    case MoveToPosition(position) if position ~ this.pos =>
      Some(ArrivedAtPosition)
      // TODO: create a rigorous method to get within radius of some position, unify with moveToDrone
    case MoveToMineralCrystal(mc)
    if (mc.position - this.pos).lengthSquared <=
      (HarvestingRange - 4) * (HarvestingRange - 4) =>
      Some(ArrivedAtMineral(mc))
    case MoveToDrone(other) =>
      val r = other.radius + drone.radius + 18
      if ((other.position - pos).lengthSquared <= r * r) {
        Some(ArrivedAtDrone(other))
      } else None
    case _ => None
  }

  private def moveToPosition(position: Vector2): Vector2 = {
    val dist = position - this.pos

    if (dist ~ Vector2.Null) {
      Vector2.Null
    } else {
      val targetOrientation = dist.orientation
      adjustOrientation(targetOrientation)
      if (targetOrientation == orientation) {
        if (dist.lengthSquared > maxSpeed * maxSpeed) maxSpeed * dist.normalized
        else dist
      } else {
        Vector2.Null
      }
    }
  }

  def recomputeVelocity(): Unit = {
    velocity =
      if (isStunned) {
        if (velocity.length <= maxSpeed * 0.10f) {
          isStunned = false
          Vector2.Null
        } else velocity * 0.9f
      } else if (drone.immobile) {
        Vector2.Null
      } else {
        _movementCommand match {
          case MoveInDirection(direction) => moveInDirection(direction)
          case MoveToPosition(position) =>
            moveToPosition(position)
          case MoveToMineralCrystal(mc) =>
            val targetDirection = (mc.position - pos).normalized
            val targetPos = mc.position - (HarvestingRange - 5) * targetDirection
            moveToPosition(targetPos)
          case MoveToDrone(other) =>
            val targetDirection = (other.position - pos).normalized
            val targetPos = other.position - (other.radius + drone.radius + 12) * targetDirection
            moveToPosition(targetPos)
          case HoldPosition =>
            Vector2.Null
        }
      }
  }

  def moveInDirection(direction: Float): Vector2 = {
    val targetOrientation = direction
    adjustOrientation(targetOrientation)

    if (targetOrientation == orientation) maxSpeed * Vector2(orientation)
    else Vector2.Null
  }

  override def handleWallCollision(areaBounds: Rectangle): Unit = {
    // find closest wall
    val dx = math.min(math.abs(pos.x - areaBounds.xMax), math.abs(pos.x - areaBounds.xMin))
    val dy = math.min(math.abs(pos.y - areaBounds.yMax), math.abs(pos.y - areaBounds.yMin))
    if (dx < dy) {
      velocity = velocity.copy(_x = -velocity.x)
      //orientation = orientation.copy(x = -orientation.x)
    } else {
      velocity = velocity.copy(_y = -velocity.y)
      //orientation = orientation.copy(y = -orientation.y)
    }
    isStunned = true
  }

  override def handleObjectCollision(other: ConstantVelocityDynamics): Unit = {
    other match {
      case other: ComputedDroneDynamics =>
        val v1 = velocity
        val v2 = other.velocity
        val x1 = pos
        val x2 = other.pos
        val w1 = weight
        val w2 = other.weight
        val v3 =  8 * (x1 - x2).normalized
        velocity = v1 - 2 * w2 / (w1 + w2) * (v1 - v2 dot x1 - x2) / (x1 - x2).lengthSquared * (x1 - x2) + v3 / w1
        other.velocity = v2 - 2 * w1 / (w1 + w2) * (v2 - v1 dot x2 - x1) / (x2 - x1).lengthSquared * (x2 - x1) - v3 / w2
        isStunned = true
        other.isStunned = true

        drone.collidedWith(other.drone)
        other.drone.collidedWith(drone)
      case missile: MissileDynamics =>
        missile.handleObjectCollision(this)
      // should probably do something here...
    }
  }

  override def toString: String = s"DroneDynamics(pos=$pos, velocity=$velocity)"

  def syncMsg(): Option[DroneMovementMsg] = {
    val positionChanged = oldPos != pos
    val orientationChanged = oldOrientation != orientation
    oldPos = pos
    oldOrientation = orientation
    if (positionChanged && orientationChanged) Some(PositionAndOrientationChanged(pos, orientation, drone.id))
    else if (positionChanged) Some(PositionChanged(pos, drone.id))
    else if (orientationChanged) Some(OrientationChanged(orientation, drone.id))
    else None
  }

  def arrivalMsg: Option[NewArrivalEvent] =
    arrivalEvent.map(event => NewArrivalEvent(event.toSerializable, drone.id))
}

private[core] case class MissileHit(
  droneID: Int,
  location: Vector2,
  missileID: Int,
  shieldDamage: Int,
  hullDamage: Int
)

private[core] case class DroneSpawned(droneID: Int)

private[core] sealed trait DroneMovementMsg {
  def droneID: Int
}

private[core] case class PositionChanged(
  newPosition: Vector2,
  droneID: Int
) extends DroneMovementMsg

private[core] case class OrientationChanged(
  newOrientation: Float,
  droneID: Int
) extends DroneMovementMsg

private[core] case class PositionAndOrientationChanged(
  newPosition: Vector2,
  newOrientation: Float,
  droneID: Int
) extends DroneMovementMsg

private[core] case class NewArrivalEvent(
  arrivalEvent: SerializableDroneEvent,
  droneID: Int
) extends DroneMovementMsg

