package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.worldstate.{MineralDescriptor, WorldObjectDescriptor}


class MineralCrystal(
  val size: Int,
  var position: Vector2,
  var harvested: Boolean = false
) extends WorldObject {
  override private[core] def descriptor: MineralDescriptor =
    MineralDescriptor(id, position.x.toFloat, position.y.toFloat, 0, size, harvested)
}

object MineralCrystal {
  def unapply(mineralCrystal: MineralCrystal): Option[(Int, Vector2)] =
    Some(mineralCrystal.size, mineralCrystal.position)
}
