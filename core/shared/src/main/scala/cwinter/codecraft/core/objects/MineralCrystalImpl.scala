package cwinter.codecraft.core.objects

import cwinter.codecraft.core.SimulatorEvent
import cwinter.codecraft.core.replay.AsInt
import cwinter.codecraft.graphics.worldstate.MineralDescriptor
import cwinter.codecraft.util.maths.{Float0To1, Vector2}


private[core] class MineralCrystalImpl(
  val size: Int,
  var position: Vector2,
  var harvested: Boolean = false,
  var harvestPosition: Vector2 = Vector2.Null,
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

  override def toString = id.toString

  def asString: String = s"MineralCrystal($size, $position)"
}

object MineralCrystalImpl {
  def unapply(mineralCrystal: MineralCrystalImpl): Option[(Int, Vector2)] =
    Some((mineralCrystal.size, mineralCrystal.position))

  private final val MineralCrystalRegex = """MineralCrystal\((\d*), (.*)\)""".r
  def fromString(string: String): MineralCrystalImpl = {
    val MineralCrystalRegex(AsInt(size), Vector2(position)) = string
    new MineralCrystalImpl(size, position)
  }
}
