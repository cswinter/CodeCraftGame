package cwinter.codecraft.graphics.worldstate

import cwinter.codecraft.graphics.models.MineralSignature
import cwinter.codecraft.util.maths
import cwinter.codecraft.util.maths.{ColorRGB, Float0To1, Rectangle}



private[codecraft] sealed trait WorldObjectDescriptor {
  val identifier: Int
  val xPos: Float
  val yPos: Float
  val orientation: Float

  def intersects(rectangle: Rectangle): Boolean = true
  @inline final protected def intersects(rectangle: Rectangle, size: Float): Boolean = {
    xPos + size > rectangle.xMin &&
      xPos - size < rectangle.xMax &&
      yPos + size > rectangle.yMin &&
      yPos - size < rectangle.yMax
  }
}

private[codecraft] case class DroneDescriptor(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,
  positions: Seq[(Float, Float, Float)],
  modules: Seq[DroneModuleDescriptor],
  hullState: Seq[Byte],
  shieldState: Option[Float],
  size: Int,

  playerColor: ColorRGB,
  constructionState: Option[Float0To1] = None,

  sightRadius: Option[Int] = None,
  inSight: Option[Iterable[(Float, Float)]] = None
) extends WorldObjectDescriptor {
  assert(hullState.size == size - 1)
  assert(!xPos.toDouble.isNaN)
  assert(!yPos.toDouble.isNaN)
  assert(!orientation.toDouble.isNaN)

  override def intersects(rectangle: Rectangle) = constructionState.nonEmpty || intersects(rectangle, 200) // FIXME
}


private[codecraft] sealed trait DroneModuleDescriptor

private[codecraft] case class StorageModuleDescriptor(positions: Seq[Int], contents: StorageModuleContents, mergingProgress: Option[Float] = None) extends DroneModuleDescriptor {
  for (x <- mergingProgress) {
    assert(x >= 0)
    assert(x <= 1)
  }
}

private[codecraft] sealed trait StorageModuleContents
private[codecraft] case object EmptyStorage extends StorageModuleContents
private[codecraft] case object MineralStorage extends StorageModuleContents
private[codecraft] case class EnergyStorage(filledPositions: Set[Int] = Set(0, 1, 2, 3, 4, 5, 6)) extends StorageModuleContents

private[codecraft] case class EnginesDescriptor(position: Int) extends DroneModuleDescriptor
private[codecraft] case class ProcessingModuleDescriptor(positions: Seq[Int], mergingProgress: Option[Int] = None) extends DroneModuleDescriptor
private[codecraft] case class ShieldGeneratorDescriptor(position: Int) extends DroneModuleDescriptor
private[codecraft] case class MissileBatteryDescriptor(position: Int, n: Int = 3) extends DroneModuleDescriptor
private[codecraft] case class ManipulatorDescriptor(position: Int) extends DroneModuleDescriptor

private[codecraft] case class EnergyGlobeDescriptor(
  xPos: Float,
  yPos: Float,
  fade: Float = 1
) extends WorldObjectDescriptor {
  assert(fade >= 0)
  assert(fade <= 1)
  val identifier: Int = 0
  val orientation: Float = 0

  override def intersects(rectangle: Rectangle): Boolean =
    intersects(rectangle, 20) // FIXME
}

private[codecraft] case class ManipulatorArm(playerColor: ColorRGB, x1: Float, y1: Float, x2: Float, y2: Float)
  extends WorldObjectDescriptor {
  val identifier: Int = 0
  val xPos: Float = 0
  val yPos: Float = 0
  val orientation: Float = 0

  override def intersects(rectangle: Rectangle): Boolean = {
    val left = math.min(x1, x2)
    val right = math.max(x1, x2)
    val bot = math.min(y1, y2)
    val top = math.max(y1, y2)
    rectangle intersects Rectangle(left, right, bot, top) // FIXME
  }
}

private[codecraft] case class MineralDescriptor(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float,

  size: Int,
  harvested: Boolean = false,
  harvestingProgress: Option[Float0To1] = None
) extends WorldObjectDescriptor {
  private[graphics] val signature = MineralSignature(size, harvested, harvestingProgress.map(_.value))

  override def intersects(rectangle: Rectangle): Boolean =
    intersects(rectangle, 0) // FIXME
}


private[codecraft] case class LightFlashDescriptor(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  stage: Float
) extends WorldObjectDescriptor {
  val orientation = 0.0f
}


private[codecraft] case class HomingMissileDescriptor(
  identifier: Int,
  positions: Seq[(Float, Float)],
  maxPos: Int,
  playerColor: ColorRGB
) extends WorldObjectDescriptor {
  val orientation = 0.0f
  val xPos = 0.0f
  val yPos = 0.0f
}

private[codecraft] case class TestingObject(time: Int) extends WorldObjectDescriptor {
  val identifier = -1
  val xPos = 0f
  val yPos = 0f
  val orientation = 0f
}

private[codecraft] case class DrawCircle(
  identifier: Int,
  xPos: Float,
  yPos: Float,
  radius: Float
) extends WorldObjectDescriptor {
  val orientation = 0.0f
}

private[codecraft] case class DrawCircleOutline(
  xPos: Float,
  yPos: Float,
  radius: Float,
  color: ColorRGB = ColorRGB(1, 1, 1)
) extends WorldObjectDescriptor {
  val identifier = -1
  val orientation = 0.0f
}

private[codecraft] case class DrawRectangle(
  identifier: Int,
  bounds: maths.Rectangle
) extends WorldObjectDescriptor {
  val orientation = 0.0f
  val xPos: Float = 0
  val yPos: Float = 0
}
