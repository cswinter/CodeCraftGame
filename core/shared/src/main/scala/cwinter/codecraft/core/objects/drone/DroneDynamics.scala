package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.util.maths.Vector2


private[core] trait DroneDynamics {
  def setTime(time: Double)
  def arrivalEvent: Option[DroneEvent]
  def update(): Unit
  def orientation: Double
  def setMovementCommand(movementCommand: MovementCommand): Boolean
  def pos: Vector2
  def remove(): Unit
  def removed: Boolean
}
