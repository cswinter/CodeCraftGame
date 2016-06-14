package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.util.maths.Vector2


private[core] trait DroneDynamics {
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

  def orientation: Float = _orientation
  def isMoving: Boolean = _movementCommand != HoldPosition

  protected def arrivalEvent: Option[DroneEvent]
  protected def halt(): Unit
  def pos: Vector2
  def setTime(time: Double)
  def update(): Unit
  def remove(): Unit
  def removed: Boolean
  def activeCommand: MovementCommand = _movementCommand
}
