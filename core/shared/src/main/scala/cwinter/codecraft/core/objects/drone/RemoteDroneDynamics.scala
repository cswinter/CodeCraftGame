package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.util.maths.Vector2

private[core] class RemoteDroneDynamics(initialPos: Vector2)
    extends DroneDynamicsBasics
    with SyncableDroneDynamics {
  private[this] var _removed: Boolean = false
  private[this] var _arrivalEvent: Option[DroneEvent] = None
  private[this] var position = initialPos

  override def setTime(time: Double): Unit = {}
  override def remove(): Unit = _removed = true
  override def removed: Boolean = _removed

  def synchronize(state: DroneMovementMsg)(implicit context: SimulationContext): Unit = state match {
    case PositionAndOrientationChanged(newPosition, newOrientation, _) =>
      position = newPosition
      _orientation = newOrientation
    case PositionChanged(newPosition, _) => position = newPosition
    case OrientationChanged(newOrientation, _) => _orientation = newOrientation
    case NewArrivalEvent(event, _) => _arrivalEvent = Some(DroneEvent(event))
  }

  override def recomputeVelocity(): Unit = _arrivalEvent = None

  override def arrivalEvent: Option[DroneEvent] = _arrivalEvent
  override def halt(): Unit = _movementCommand = HoldPosition
  override def pos: Vector2 = position
}
