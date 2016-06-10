package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.util.maths.Vector2


private[core] class RemoteDroneDynamics(
  initialPosition: Vector2
) extends DroneDynamics {
  private[this] var position: Vector2 = initialPosition
  private[this] var _orientation: Float = 0
  private[this] var _removed: Boolean = false
  private[this] var _arrivalEvent: Option[DroneEvent] = None
  private[this] var _isMoving: Boolean = false

  private[this] var stateIsFresh: Boolean = true


  override def setTime(time: Double): Unit = {}
  override def remove(): Unit = _removed = true
  override def setMovementCommand(movementCommand: MovementCommand): Boolean = {
    val redundant = movementCommand match {
      case MoveToPosition(p) if p ~ pos => true
      case _ => false
    }
    _isMoving = !(redundant || movementCommand == HoldPosition)
    redundant
  }

  override def removed: Boolean = _removed

  def update(state: DroneDynamicsState)(implicit context: SimulationContext): Unit = {
    position = state.position
    _orientation = state.orientation
    _arrivalEvent = state.arrivalEvent.map(DroneEvent(_))
    if (_arrivalEvent.nonEmpty) _isMoving = false
    stateIsFresh = true
  }

  override def update(): Unit = {
    assert(stateIsFresh, "Trying to update() RemoteDroneDynamics, but new state has not been received yet.")
    stateIsFresh = false
  }

  override def checkArrivalConditions(): Option[DroneEvent] = _arrivalEvent
  override def orientation: Float = _orientation
  override def pos: Vector2 = position

  override def isMoving: Boolean = _isMoving
}

