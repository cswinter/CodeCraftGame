package cwinter.codinggame.core.objects.drone

import cwinter.codinggame.core.api.{DroneHandle, DroneSpec}
import cwinter.codinggame.core.errors.{ObjectNotVisibleException, Errors}
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.Player

/**
 * Wrapper around drone class to allow users to query a subset of properties of enemy drones.
 */
class EnemyDroneHandle(
  private[core] val drone: Drone,
  private val holder: Player // the player to whom the handle is given
) extends DroneHandle {
  private[this] var _lastKnownPosition: Vector2 = drone.position // TODO: implement


  override def position: Vector2 = {
    if (isVisible) {
      drone.position
    } else {
      val exception = new ObjectNotVisibleException(
        "Cannot get the position of an enemy drone that is not inside the sight radius of any of your drones.")
      Errors.error(exception, drone.position)
    }
  }

  override def weaponsCooldown: Int = {
    if (isVisible) {
      drone.weaponsCooldown
    } else {
      val exception = new ObjectNotVisibleException(
        "Cannot get the weapons cooldown of an enemy that is not inside the sight radius of any of your drones.")
      Errors.error(exception, drone.position)
    }
  }

  def isVisible: Boolean = {
    drone.player == holder ||
    drone.objectsInSight.exists {
      case drone: Drone => drone.player == holder
      case _ => false
    }
  }

  override def spec: DroneSpec = drone.spec
  override def player: Player = drone.player
}
