package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.util.maths.Vector2

private[core] trait DroneDynamics {
  def setMovementCommand(command: MovementCommand): Boolean
  def checkArrivalConditions(): Option[DroneEvent]
  def pos: Vector2
  def setTime(time: Double)
  def recomputeVelocity(): Unit
  def remove(): Unit
  def removed: Boolean
  def orientation: Float
  def isMoving: Boolean
  def activeCommand: MovementCommand
  def isStunned: Boolean
}

private[core] trait SyncableDroneDynamics {
  def synchronize(state: DroneMovementMsg)(implicit context: SimulationContext): Unit
}

private[core] trait DroneDynamicsBasics extends DroneDynamics {
  protected var _movementCommand: MovementCommand = HoldPosition
  protected var _orientation: Float = 0

  def setMovementCommand(command: MovementCommand): Boolean = {
    command match {
      case MoveToPosition(p) if p ~ pos => return true
      case _ =>
    }
    val redundant = command == _movementCommand
    _movementCommand = command
    redundant
  }

  def checkArrivalConditions(): Option[DroneEvent] = {
    val event = arrivalEvent
    if (event.nonEmpty) halt()
    event
  }

  private[drone] def orientation_=(value: Float) = _orientation = value
  def orientation: Float = _orientation
  def isMoving: Boolean = _movementCommand != HoldPosition
  def activeCommand: MovementCommand = _movementCommand

  protected def arrivalEvent: Option[DroneEvent]
  protected def halt(): Unit
}
