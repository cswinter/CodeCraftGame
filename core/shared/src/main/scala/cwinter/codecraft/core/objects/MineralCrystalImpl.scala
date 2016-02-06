package cwinter.codecraft.core.objects

import cwinter.codecraft.core.SimulatorEvent
import cwinter.codecraft.core.api.{MineralCrystal, Player}
import cwinter.codecraft.core.replay.AsInt
import cwinter.codecraft.graphics.worldstate.MineralDescriptor
import cwinter.codecraft.util.maths.{Float0To1, Vector2}


private[core] class MineralCrystalImpl(
  var size: Int,
  val id: Int,
  private[this] var _position: Vector2,
  private[this] var _harvested: Boolean = false
) extends WorldObject {
  private[this] var _descriptor = Seq(createDescriptor)
  private var handles = Map.empty[Player, MineralCrystal]

  def position: Vector2 = _position
  def position_=(value: Vector2): Unit = { _position = value; updateDescriptor() }
  def harvested: Boolean = _harvested
  def decreaseSize(): Unit = {
    size -= 1
    if (size == 0) _harvested = true
    updateDescriptor()
  }

  @inline final private[this] def updateDescriptor(): Unit = _descriptor = Seq(createDescriptor)

  private def createDescriptor: MineralDescriptor =
      MineralDescriptor(id, position.x.toFloat, position.y.toFloat, 0, size, harvested)

  override private[core] def descriptor: Seq[MineralDescriptor] = _descriptor

  override private[core] def isDead = false

  override def update(): Seq[SimulatorEvent] = Seq.empty[SimulatorEvent]

  override def toString = id.toString

  def getHandle(player: Player): MineralCrystal = {
    if (!handles.contains(player))
      handles += player -> new MineralCrystal(this, player)
    handles(player)
  }

  def asString: String = s"MineralCrystal($size, $position)"
}

object MineralCrystalImpl {
  def unapply(mineralCrystal: MineralCrystalImpl): Option[(Int, Vector2)] =
    Some((mineralCrystal.size, mineralCrystal.position))
}
