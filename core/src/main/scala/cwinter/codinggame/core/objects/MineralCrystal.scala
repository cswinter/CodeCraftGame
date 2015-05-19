package cwinter.codinggame.core.objects

import cwinter.codinggame.core.SimulatorEvent
import cwinter.codinggame.util.maths.{Float0To1, Vector2}
import cwinter.codinggame.worldstate.MineralDescriptor


private[core] class MineralCrystal(
  val size: Int,
  var position: Vector2,
  var harvested: Boolean = false,
  var harvestPosition: Vector2 = Vector2.NullVector,
  var harvestProgress: Option[Float0To1] = None
) extends WorldObject {

  override private[core] def descriptor: Seq[MineralDescriptor] = Seq(
    harvestProgress match {
      case Some(_) =>
        MineralDescriptor(
          id, harvestPosition.x.toFloat, harvestPosition.y.toFloat,
          0, size, harvested, harvestProgress)
      case None =>
        MineralDescriptor(id, position.x.toFloat, position.y.toFloat, 0, size, harvested)
    }
  )

  override private[core] def hasDied = false

  override def update(): Seq[SimulatorEvent] = Seq.empty[SimulatorEvent]
}

object MineralCrystal {
  def unapply(mineralCrystal: MineralCrystal): Option[(Int, Vector2)] =
    Some((mineralCrystal.size, mineralCrystal.position))
}
