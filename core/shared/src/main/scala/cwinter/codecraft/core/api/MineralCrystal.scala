package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.graphics.worldstate.Player
import cwinter.codecraft.util.maths.Vector2

import scala.scalajs.js.annotation.JSExportAll


/**
 * A mineral crystal. Can be harvested and processed to generate resources.
 */
@JSExportAll
class MineralCrystal(
  private[core] val mineralCrystal: MineralCrystalImpl,
  private[core] val holder: Player
) {
  // TODO: implement isVisible and use to restrict access
  def position: Vector2 = mineralCrystal.position
  def size: Int = mineralCrystal.size
  def harvested: Boolean = mineralCrystal.harvested
}

