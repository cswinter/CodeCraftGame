package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.MineralDescriptor


class MineralCrystal(
  val size: Int,
  var position: Vector2,
  var harvested: Boolean = false
) extends WorldObject {
  override private[core] def descriptor: Seq[MineralDescriptor] = Seq(
    MineralDescriptor(id, position.x.toFloat, position.y.toFloat, 0, size, harvested)
  )

  override private[core] def hasDied = false

  override def update(): Seq[SimulatorEvent] = Seq.empty[SimulatorEvent]
}

object MineralCrystal {
  def unapply(mineralCrystal: MineralCrystal): Option[(Int, Vector2)] =
    Some((mineralCrystal.size, mineralCrystal.position))
}
