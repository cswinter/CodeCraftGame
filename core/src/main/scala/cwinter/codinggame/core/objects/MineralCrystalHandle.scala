package cwinter.codinggame.core.objects

import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.Player


/**
 * Wrapper around mineral class to allow for restricted access to mineral properties.
 */
class MineralCrystalHandle(
  private[core] val mineralCrystal: MineralCrystal,
  private[core] val holder: Player
) {
  // TODO: implement isVisible and use to restrict access
  def position: Vector2 = mineralCrystal.position
  def size: Int = mineralCrystal.size
  def harvested: Boolean = mineralCrystal.harvested
}

