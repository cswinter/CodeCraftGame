package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.worldstate.{MineralDescriptor, WorldObjectDescriptor}


case class MineralCrystal(
  size: Int,
  position: Vector2
) extends WorldObject {
  override private[core] def descriptor: MineralDescriptor =
    MineralDescriptor(id, position.x.toFloat, position.y.toFloat, 0, size)
}
