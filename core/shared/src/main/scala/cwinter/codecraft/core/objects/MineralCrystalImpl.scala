package cwinter.codecraft.core.objects

import cwinter.codecraft.collisions.PassiveVisionTracking
import cwinter.codecraft.core.SimulatorEvent
import cwinter.codecraft.core.api.{MineralCrystal, Player}
import cwinter.codecraft.core.graphics.MineralCrystalModel
import cwinter.codecraft.core.objects.drone.StorageModule
import cwinter.codecraft.graphics.engine.{ModelDescriptor, NullPositionDescriptor}
import cwinter.codecraft.util.maths.{GlobalRNG, RNG, Vector2}


private[core] class MineralCrystalImpl(
  var size: Int,
  val id: Int,
  private[this] var _position: Vector2,
  private[this] var _harvested: Boolean = false
) extends WorldObject with PassiveVisionTracking {
  def maxSpeed = 0

  private[this] val orientation = (2 * math.Pi * GlobalRNG.double()).toFloat
  private[this] var _descriptor = Seq(createDescriptor)
  private var handles = Map.empty[Player, MineralCrystal]
  private[objects] var claimedBy: Option[StorageModule] = None


  def position: Vector2 = _position
  def position_=(value: Vector2): Unit = { _position = value; updateDescriptor() }
  def harvested: Boolean = _harvested
  def decreaseSize(): Unit = {
    size -= 1
    if (size == 0) _harvested = true
    updateDescriptor()
  }

  @inline final private[this] def updateDescriptor(): Unit = _descriptor = Seq(createDescriptor)

  private def createDescriptor: ModelDescriptor[Unit] =
    ModelDescriptor(
      NullPositionDescriptor,
      MineralCrystalModel(size, position.x.toFloat, position.y.toFloat, orientation)
    )

  override private[core] def descriptor: Seq[ModelDescriptor[Unit]] = _descriptor

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
