package cwinter.codecraft.core.objects

import cwinter.codecraft.core.SimulatorEvent
import cwinter.codecraft.core.replay.AsInt
import cwinter.codecraft.graphics.worldstate.MineralDescriptor
import cwinter.codecraft.util.maths.{Float0To1, Vector2}


private[core] class MineralCrystalImpl(
  val size: Int,
  val id: Int,
  private[this] var _position: Vector2,
  private[this] var _harvested: Boolean = false,
  private[this] var _harvestPosition: Vector2 = Vector2.Null,
  private[this] var _harvestProgress: Option[Float0To1] = None
) extends WorldObject {
  private[this] var _descriptor = Seq(createDescriptor)

  def position: Vector2 = _position
  def position_=(value: Vector2): Unit = { _position = value; updateDescriptor() }
  def harvested: Boolean = _harvested
  def harvested_=(value: Boolean): Unit = { _harvested = value; updateDescriptor() }
  def harvestPosition: Vector2 = _harvestPosition
  def harvestPosition_=(value: Vector2): Unit = { _harvestPosition = value; updateDescriptor() }
  def harvestProgress: Option[Float0To1] = _harvestProgress
  def harvestProgress_=(value: Option[Float0To1]) = { _harvestProgress = value; updateDescriptor() }


  @inline final private[this] def updateDescriptor(): Unit = _descriptor = Seq(createDescriptor)

  private def createDescriptor: MineralDescriptor = harvestProgress match {
    case Some(_) =>
      MineralDescriptor(
        id, harvestPosition.x.toFloat, harvestPosition.y.toFloat,
        0, size, harvested, harvestProgress)
    case None =>
      MineralDescriptor(id, position.x.toFloat, position.y.toFloat, 0, size, harvested)
  }

  override private[core] def descriptor: Seq[MineralDescriptor] = _descriptor

  override private[core] def isDead = false

  override def update(): Seq[SimulatorEvent] = Seq.empty[SimulatorEvent]

  override def toString = id.toString

  def asString: String = s"MineralCrystal($size, $position)"
}

object MineralCrystalImpl {
  def unapply(mineralCrystal: MineralCrystalImpl): Option[(Int, Vector2)] =
    Some((mineralCrystal.size, mineralCrystal.position))
}
