package cwinter.codecraft.graphics.worldstate

import cwinter.codecraft.graphics.models.MineralSignature
import cwinter.codecraft.util.maths
import cwinter.codecraft.util.maths.{Vector2, ColorRGB, Float0To1, Rectangle}



private[codecraft] case class ModelDescriptor(
  xPos: Float,
  yPos: Float,
  orientation: Float,
  objectDescriptor: WorldObjectDescriptor
) {
  assert(!xPos.toDouble.isNaN)
  assert(!yPos.toDouble.isNaN)
  assert(!orientation.toDouble.isNaN)

  @inline final def intersects(rectangle: Rectangle): Boolean =
    objectDescriptor.intersects(xPos, yPos, rectangle)
}

private[codecraft] sealed trait WorldObjectDescriptor {
  def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean = true
  @inline final protected def intersects(
    xPos: Float, yPos: Float, rectangle: Rectangle, size: Float
  ): Boolean = {
    xPos + size > rectangle.xMin &&
    xPos - size < rectangle.xMax &&
    yPos + size > rectangle.yMin &&
    yPos - size < rectangle.yMax
  }
}

private[codecraft] case class DroneDescriptor(
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

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle) =
    constructionState.nonEmpty || intersects(xPos, yPos, rectangle, 200) // FIXME
}


private[codecraft] sealed trait DroneModuleDescriptor

private[codecraft] case class StorageModuleDescriptor(
  position: Int,
  contents: StorageModuleContents,
  relativeMineralPosition: Option[Vector2] = None
) extends DroneModuleDescriptor


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
  fade: Float = 1
) extends WorldObjectDescriptor {
  assert(fade >= 0)
  assert(fade <= 1)

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean =
    intersects(xPos, yPos, rectangle, 20) // FIXME
}

private[codecraft] case class ManipulatorArm(playerColor: ColorRGB, x1: Float, y1: Float, x2: Float, y2: Float)
  extends WorldObjectDescriptor {

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean = {
    val left = math.min(x1, x2)
    val right = math.max(x1, x2)
    val bot = math.min(y1, y2)
    val top = math.max(y1, y2)
    rectangle intersects Rectangle(left, right, bot, top) // FIXME
  }
}

private[codecraft] case class MineralDescriptor(size: Int) extends WorldObjectDescriptor {
  private[graphics] val signature = MineralSignature(size)

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean =
    intersects(xPos, yPos, rectangle, 0) // FIXME
}


private[codecraft] case class LightFlashDescriptor(stage: Float) extends WorldObjectDescriptor


private[codecraft] case class HomingMissileDescriptor(
  positions: Seq[(Float, Float)],
  maxPos: Int,
  playerColor: ColorRGB
) extends WorldObjectDescriptor

private[codecraft] case class TestingObject(time: Int) extends WorldObjectDescriptor

private[codecraft] case class DrawCircle(
  radius: Float,
  identifier: Int
) extends WorldObjectDescriptor


private[codecraft] case class DrawCircleOutline(
  radius: Float,
  color: ColorRGB = ColorRGB(1, 1, 1)
) extends WorldObjectDescriptor


private[codecraft] case class DrawRectangle(
  bounds: maths.Rectangle
) extends WorldObjectDescriptor

