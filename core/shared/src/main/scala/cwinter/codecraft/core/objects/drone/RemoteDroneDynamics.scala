package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.util.maths.Vector2


private[core] class RemoteDroneDynamics(initialPos: Vector2) extends DroneDynamics {
  private[this] var _removed: Boolean = false
  private[this] var _arrivalEvent: Option[DroneEvent] = None
  private[this] var stateIsFresh: Boolean = true
  private[this] var position = initialPos


  override def setTime(time: Double): Unit = {}
  override def remove(): Unit = _removed = true
  override def removed: Boolean = _removed

  def synchronize(state: DroneDynamicsState)(implicit context: SimulationContext): Unit = {
    position = state.position
    _orientation = state.orientation
    _arrivalEvent = state.arrivalEvent.map(DroneEvent(_))
    stateIsFresh = true
  }

  override def update(): Unit = {
    assert(stateIsFresh, "Trying to update() RemoteDroneDynamics, but new state has not been received yet.")
    _arrivalEvent = None
    stateIsFresh = false
  }

  override def arrivalEvent: Option[DroneEvent] = _arrivalEvent
  override def halt(): Unit = _movementCommand = HoldPosition
  override def pos: Vector2 = position
}

