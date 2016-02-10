package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.objects.{ConstantVelocityDynamics, MissileDynamics}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}


private[core] class ComputedDroneDynamics(
  val drone: DroneImpl,
  val maxSpeed: Double,
  val weight: Double,
  radius: Double,
  initialPosition: Vector2,
  initialTime: Double
) extends ConstantVelocityDynamics(
  radius,
  drone.player.id,
  true,
  initialPosition,
  initialTime
) with DroneDynamics {

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
    require(limit <= maxSpeed)
    speed = limit
  }

  private def halt(): Unit = {
    if (!isStunned) {
      velocity = Vector2.Null
    }
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

  def checkArrivalConditions(): Option[DroneEvent] = {
    val event = arrivalEvent
    val hasArrived = event.nonEmpty
    if (hasArrived) halt()
    event
  }

  def arrivalEvent: Option[DroneEvent] = _movementCommand match {
    case MoveToPosition(position) if position ~ this.pos =>
      Some(ArrivedAtPosition)
      // TODO: create a rigorous method to get within radius of some position, unify with moveToDrone
    case MoveToMineralCrystal(mc)
    if (mc.position - this.pos).lengthSquared <=
      (DroneConstants.HarvestingRange - 4) * (DroneConstants.HarvestingRange - 4) =>
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
        val speed = maxSpeed / 30 // TODO: improve this
        if ((dist dot dist) > speed * speed) {
          maxSpeed * dist.normalized
        } else {
          val distance = dist.length
          distance * 30 * dist.normalized
        }
      } else {
        Vector2.Null
      }
    }
  }

  override def update(): Unit = {
    velocity =
      if (isStunned) {
        if (velocity.length <= maxSpeed * 0.05f) {
          isStunned = false
          Vector2.Null
        } else velocity * 0.9f
      } else if (drone.immobile) {
        Vector2.Null
      } else {
        _movementCommand match {
          case MoveInDirection(direction) =>
            val targetOrientation = direction
            adjustOrientation(targetOrientation)

            if (targetOrientation == orientation) {
              maxSpeed * Vector2(orientation)
            } else {
              Vector2.Null
            }
          case MoveToPosition(position) =>
            moveToPosition(position)
          case MoveToMineralCrystal(mc) =>
            val targetDirection = (mc.position - pos).normalized
            val targetPos = mc.position - (DroneConstants.HarvestingRange - 5) * targetDirection
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

  override def handleWallCollision(areaBounds: Rectangle): Unit = {
    // find closest wall
    val dx = math.min(math.abs(pos.x + areaBounds.xMax), math.abs(pos.x + areaBounds.xMin))
    val dy = math.min(math.abs(pos.y + areaBounds.yMax), math.abs(pos.y + areaBounds.yMin))
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
        velocity = v1 - 2 * w2 / (w1 + w2) * (v1 - v2 dot x1 - x2) / (x1 - x2).lengthSquared * (x1 - x2)
        other.velocity = v2 - 2 * w1 / (w1 + w2) * (v2 - v1 dot x2 - x1) / (x2 - x1).lengthSquared * (x2 - x1)
        isStunned = true
        other.isStunned = true
      case missile: MissileDynamics =>
        missile.handleObjectCollision(this)
      // should probably do something here...
    }
  }

  def isMoving = _movementCommand != HoldPosition

  override def toString: String = s"DroneDynamics(pos=$pos, velocity=$velocity)"
  
  
  def state: DroneDynamicsState = DroneDynamicsState(pos, orientation, arrivalEvent.map(_.toSerializable), drone.id)
}


import upickle.default.key
sealed trait DroneStateMessage

@key("Hit") case class MissileHit(
  droneID: Int,
  location: Vector2,
  missileID: Int
) extends DroneStateMessage

@key("State") case class DroneDynamicsState(
  position: Vector2,
  orientation: Double,
  arrivalEvent: Option[SerializableDroneEvent],
  droneId: Int
) extends DroneStateMessage

