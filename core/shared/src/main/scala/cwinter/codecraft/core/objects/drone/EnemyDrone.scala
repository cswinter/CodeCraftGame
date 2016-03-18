package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.{Player, ObjectNotVisibleException, Drone, DroneSpec}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.util.PrecomputedHashcode
import cwinter.codecraft.util.maths.Vector2

/**
 * Wrapper around drone class to allow users to query a subset of properties of enemy drones.
 */
private[core] class EnemyDrone(
  private[core] val drone: DroneImpl,
  private val holder: Player // the player to whom the handle is given
) extends Drone {
  private[this] var _lastKnownPosition: Vector2 = drone.position
  private[this] var _lastKnownOrientation: Double = drone.dynamics.orientation


  override def lastKnownPosition: Vector2 = _lastKnownPosition
  override def lastKnownOrientation: Double = _lastKnownOrientation
  override def isEnemy: Boolean = true
  override def isVisible: Boolean =
    drone.player == holder || drone.dronesInSight.exists(_.drone.player == holder)

  private[core] def recordPosition(): Unit =
    if (isVisible) {
      _lastKnownPosition = position
      _lastKnownOrientation = drone.dynamics.orientation
    }
}
