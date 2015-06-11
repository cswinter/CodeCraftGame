package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.drone.Drone
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.worldstate.Player


trait DroneHandle {
  def position: Vector2
  def spec: DroneSpec
  def weaponsCooldown: Int
  def isVisible: Boolean
  def player: Player
  def hitpoints: Int
  def isEnemy: Boolean

  private[core] def drone: Drone
}
