package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.{Player, ObjectNotVisibleException, Drone, DroneSpec}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.util.maths.Vector2

/**
 * Wrapper around drone class to allow users to query a subset of properties of enemy drones.
 */
private[core] class EnemyDrone(
  private[core] val drone: DroneImpl,
  private val holder: Player // the player to whom the handle is given
) extends Drone {
  private[this] var _lastKnownPosition: Vector2 = drone.position // TODO: implement


  override def position: Vector2 = {
    if (isVisible) {
      drone.position
    } else {
      val exception = new ObjectNotVisibleException(
        "Cannot get the position of an enemy drone that is not inside the sight radius of any of your drones. " +
          "Use lastKnownPosition instead.")
      Errors.error(exception, drone.position)
    }
  }

  override def missileCooldown: Int = {
    if (isVisible) {
      drone.weaponsCooldown
    } else {
      val exception = new ObjectNotVisibleException(
        "Cannot get the weapons cooldown of an enemy that is not inside the sight radius of any of your drones.")
      Errors.error(exception, drone.position)
    }
  }

  override def hitpoints: Int = {
    if (isVisible) {
      drone.hitpoints
    } else {
      val exception = new ObjectNotVisibleException(
        "Cannot get the hitpoints of an enemy drone that is not inside the sight radius of any of your drones.")
      Errors.error(exception, drone.position)
    }
  }

  override def isDead: Boolean = drone.isDead

  /**
    * The position of this drone at the last time it was seen by any of your drones.
    */
  override def lastKnownPosition: Vector2 = _lastKnownPosition

  @deprecated("The `storedResources` method now returns the same result and should be used instead.", "0.2.4.0")
  def totalAvailableResources: Int = storedResources

  def storedResources: Int = drone.storedResources


  override def isEnemy: Boolean = true

  private[core] def recordPosition(): Unit =
    if (isVisible) _lastKnownPosition = position

  def isVisible: Boolean = {
    drone.player == holder ||
    drone.objectsInSight.exists {
      case drone: DroneImpl => drone.player == holder
      case _ => false
    }
  }

  override def spec: DroneSpec = drone.spec
  override def playerID: Int = drone.player.id
}
