package cwinter.codecraft.graphics.worldstate

import cwinter.codecraft.graphics.model.Model
import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.util.maths.{ColorRGB, Float0To1, Rectangle, Vector2}
import cwinter.codecraft.util.{PrecomputedHashcode, maths}


private[codecraft] case class ModelDescriptor[T](
  position: PositionDescriptor,
  objectDescriptor: WorldObjectDescriptor[T],
  objectParameters: T
) {
  @inline final def intersects(rectangle: Rectangle): Boolean =
    objectDescriptor.intersects(position.x, position.y, rectangle)
}

object ModelDescriptor {
  def apply(position: PositionDescriptor, objectDescriptor: WorldObjectDescriptor[Unit]): ModelDescriptor[Unit] =
    ModelDescriptor(position, objectDescriptor, Unit)
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

private[codecraft] sealed trait WorldObjectDescriptor[T] extends PrecomputedHashcode {
  self: Product =>

  def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean = true
  @inline final protected def intersects(
    xPos: Float, yPos: Float, rectangle: Rectangle, size: Float
  ): Boolean = {
    xPos + size > rectangle.xMin &&
    xPos - size < rectangle.xMax &&
    yPos + size > rectangle.yMin &&
    yPos - size < rectangle.yMax
  }


  private[this] var _cachedModel: Option[Model[T]] = None
  private[graphics] def cachedModel_=(value: Model[T]): Unit = _cachedModel = Some(value)
  private[graphics] def cachedModel: Option[Model[T]] = _cachedModel
}


private[codecraft] case class DroneDescriptor(
  sides: Int,
  modules: Seq[DroneModuleDescriptor],
  hasShields: Boolean,
  hullState: Seq[Byte],
  isBuilding: Boolean,
  animationTime: Int,
  playerColor: ColorRGB
) extends WorldObjectDescriptor[DroneModelParameters] {
  assert(hullState.size == sides - 1)

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle) =
    intersects(xPos, yPos, rectangle, 200) // FIXME
}

private[codecraft] case class DroneModelParameters(
  shieldState: Option[Float],
  constructionState: Option[Float0To1] = None
)


private[codecraft] sealed trait DroneModuleDescriptor

private[codecraft] case class StorageModuleDescriptor(
  position: Int,
  contents: StorageModuleContents
) extends DroneModuleDescriptor


private[codecraft] sealed trait StorageModuleContents
private[codecraft] case object EmptyStorage extends StorageModuleContents
private[codecraft] case object MineralStorage extends StorageModuleContents
private[codecraft] case class EnergyStorage(filledPositions: Set[Int] = Set(0, 1, 2, 3, 4, 5, 6)) extends StorageModuleContents

private[codecraft] case class EnginesDescriptor(position: Int) extends DroneModuleDescriptor
private[codecraft] case class ShieldGeneratorDescriptor(position: Int) extends DroneModuleDescriptor
private[codecraft] case class MissileBatteryDescriptor(position: Int, n: Int = 3) extends DroneModuleDescriptor
private[codecraft] case class ManipulatorDescriptor(position: Int) extends DroneModuleDescriptor

private[codecraft] case class HarvestingBeamsDescriptor(
  droneSize: Int,
  moduleIndices: Seq[Int],
  mineralDisplacement: Vector2
) extends WorldObjectDescriptor[Unit]

private[codecraft] case class ConstructionBeamDescriptor(
  droneSize: Int,
  modules: Seq[(Int, Boolean)],
  constructionDisplacement: Vector2,
  playerColor: ColorRGB
) extends WorldObjectDescriptor[Unit]

private[codecraft] case class EnergyGlobeDescriptor(
  fade: Float
) extends WorldObjectDescriptor[Unit] {
  assert(fade >= 0)
  assert(fade <= 1)

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean =
    intersects(xPos, yPos, rectangle, 20) // FIXME
}

private[codecraft] object PlainEnergyGlobeDescriptor extends EnergyGlobeDescriptor(1)

private[codecraft] case class MineralDescriptor(size: Int, xPos: Float, yPos: Float, orientation: Float)
    extends WorldObjectDescriptor[Unit] {
  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean =
    intersects(this.xPos, this.yPos, rectangle, 50)
}


private[codecraft] case class LightFlashDescriptor(stage: Float) extends WorldObjectDescriptor[LightFlashDescriptor]


private[codecraft] case class HomingMissileDescriptor(
  positions: Seq[(Float, Float)],
  maxPos: Int,
  playerColor: ColorRGB
) extends WorldObjectDescriptor[Unit]

private[codecraft] case class BasicHomingMissileDescriptor(
  x: Float,
  y: Float,
  playerColor: ColorRGB
) extends WorldObjectDescriptor[Unit]

private[codecraft] case class TestingObject(time: Int) extends WorldObjectDescriptor[TestingObject]

private[codecraft] case class DrawCircle(
  radius: Float,
  identifier: Int
) extends WorldObjectDescriptor[Unit]


private[codecraft] case class DrawCircleOutline(
  radius: Float,
  color: ColorRGB = ColorRGB(1, 1, 1)
) extends WorldObjectDescriptor[Unit]


private[codecraft] case class DrawRectangle(
  bounds: maths.Rectangle
) extends WorldObjectDescriptor[Unit]

