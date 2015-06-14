package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone.DroneImpl
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.worldstate.Player


trait Drone {
  /**
   * Returns the drones position.
   */
  def position: Vector2

  /**
   * Returns the drones module specification.
   */
  def spec: DroneSpec

  /**
   * Returns the drones homing missile cooldown.
   */
  def weaponsCooldown: Int

  /**
   * Returns whether the drone is within the sight radius of any of your drones.
   */
  def isVisible: Boolean

  /**
   * Returns the player who commands this drone.
   */
  def player: Player

  /**
   * Returns the current number of hitpoints.
   */
  def hitpoints: Int

  /**
   * Returns true if this drone is an enemy, false if it is one of your own drones.
   */
  def isEnemy: Boolean

  private[core] def drone: DroneImpl
}
