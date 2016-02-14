package cwinter.codecraft.graphics.worldstate

import cwinter.codecraft.graphics.model.Model
import cwinter.codecraft.graphics.models.MineralSignature
import cwinter.codecraft.util.maths
import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.util.maths.{Vector2, ColorRGB, Float0To1, Rectangle}



private[codecraft] case class ModelDescriptor(
  position: PositionDescriptor,
  objectDescriptor: WorldObjectDescriptor
) {
  @inline final def intersects(rectangle: Rectangle): Boolean =
    objectDescriptor.intersects(position.x, position.y, rectangle)
}

private[codecraft] case class PositionDescriptor(
  x: Float,
  y: Float,
  orientation: Float = 0
) {
  assert(!x.toDouble.isNaN)
  assert(!y.toDouble.isNaN)
  assert(!orientation.toDouble.isNaN)

  private[this] var _cachedModelviewMatrix: Option[Matrix4x4] = None
  private[graphics] def cachedModelviewMatrix_=(value: Matrix4x4): Unit =
    _cachedModelviewMatrix = Some(value)
  private[graphics] def cachedModelviewMatrix = _cachedModelviewMatrix
}

private[codecraft] object NullPositionDescriptor extends PositionDescriptor(0, 0, 0)

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


  private[this] var _cachedModel: Option[Model[_]] = None
  private[graphics] def cachedModel_=(value: Model[_]): Unit = _cachedModel = Some(value)
  private[graphics] def cachedModel: Option[Model[_]] = _cachedModel
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
private[codecraft] case class ManipulatorDescriptor(
  position: Int,
  relativeConstructionPosition: Option[Vector2],
  active: Boolean
) extends DroneModuleDescriptor

private[codecraft] case class EnergyGlobeDescriptor(
  fade: Float
) extends WorldObjectDescriptor {
  assert(fade >= 0)
  assert(fade <= 1)

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean =
    intersects(xPos, yPos, rectangle, 20) // FIXME
}

object PlainEnergyGlobeDescriptor extends EnergyGlobeDescriptor(1)

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

