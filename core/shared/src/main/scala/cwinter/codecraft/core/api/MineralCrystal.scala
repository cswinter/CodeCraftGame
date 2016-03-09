package cwinter.codecraft.core.api

import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.util.maths.Vector2

import scala.scalajs.js.annotation.JSExportAll


/**
  * A mineral crystal.
  * Can be harvested by drones with storage modules to obtain resources.
  */
@JSExportAll
class MineralCrystal(
  private[core] val mineralCrystal: MineralCrystalImpl,
  private[core] val holder: Player
) {
  def position: Vector2 = mineralCrystal.position
  def size: Int = mineralCrystal.size
  def harvested: Boolean = mineralCrystal.harvested
}

