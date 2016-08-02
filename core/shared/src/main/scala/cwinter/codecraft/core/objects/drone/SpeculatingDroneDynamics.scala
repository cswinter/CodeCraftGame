package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.util.maths.Vector2

private[core] class SpeculatingDroneDynamics(
  val remote: RemoteDroneDynamics,
  val speculative: ComputedDroneDynamics
) extends DroneDynamics
    with SyncableDroneDynamics {

  override def setMovementCommand(command: MovementCommand): Boolean = {
    remote.setMovementCommand(command)
    speculative.setMovementCommand(command)
  }

  override def synchronize(state: DroneMovementMsg)(implicit context: SimulationContext): Unit = {
    remote.synchronize(state)
  }

  def syncSpeculator(): Boolean = {
    if (remote.orientation != speculative.orientation) speculative.orientation = remote.orientation
    if (remote.pos != speculative.pos) {
      speculative.setPosition(remote.pos)
      true
    } else false
  }

  override def activeCommand: MovementCommand = remote.activeCommand
  override def orientation: Float = speculative.orientation
  override def checkArrivalConditions(): Option[DroneEvent] = {
    speculative.checkArrivalConditions()
    remote.checkArrivalConditions()
  }
  override def isMoving: Boolean = remote.isMoving
  override def removed: Boolean = remote.removed
  override def remove(): Unit = {
    remote.remove()
    speculative.remove()
  }
  override def pos: Vector2 = speculative.pos
  override def setTime(time: Double): Unit = speculative.setTime(time)
  override def recomputeVelocity(): Unit = {
    remote.recomputeVelocity()
    speculative.recomputeVelocity()
  }
}
