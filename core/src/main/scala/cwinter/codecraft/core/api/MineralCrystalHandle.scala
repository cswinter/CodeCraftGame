package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.MineralCrystal
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.worldstate.Player


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

