package cwinter.codinggame.core.drone

import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.Player


trait DroneHandle {
  def position: Vector2
  def spec: DroneSpec
  def weaponsCooldown: Int
  def isVisible: Boolean
  def player: Player

  private[core] def drone: Drone
}
