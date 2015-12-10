package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.util.maths.Vector2


private[core] class RemoteDroneDynamics(
  initialPosition: Vector2
) extends DroneDynamics {
  private[this] var position: Vector2 = initialPosition
  private[this] var _orientation: Double = 0
  private[this] var _removed: Boolean = false
  private[this] var _arrivalEvent: Option[DroneEvent] = None

  private[this] var stateIsFresh: Boolean = true


  override def setTime(time: Double): Unit = {}
  override def remove(): Unit = _removed = true
  override def setMovementCommand(movementCommand: MovementCommand): Boolean = true
  override def removed: Boolean = _removed

  def update(state: DroneDynamicsState): Unit = {
    position = state.position
    _orientation = state.orientation
    _arrivalEvent = state.arrivalEvent
    stateIsFresh = true
  }

  override def update(): Unit = {
    assert(stateIsFresh, "Trying to update() RemoteDroneDynamics, but new state has not been received yet.")
    stateIsFresh = false
  }

  override def checkArrivalConditions(): Option[DroneEvent] = _arrivalEvent
  override def orientation: Double = _orientation
  override def pos: Vector2 = position
}

