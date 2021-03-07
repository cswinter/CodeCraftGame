package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.{GraphicsContext, WorldObjectDescriptor}
import cwinter.codecraft.graphics.materials.Intensity
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.{Polygon, PolygonRing}
import cwinter.codecraft.util.maths.Geometry.circumradius
import cwinter.codecraft.util.maths._
import cwinter.codecraft.util.modules.ModulePosition

import scala.math._

private[codecraft] case class DroneModel(
  sides: Int,
  modules: Seq[DroneModuleDescriptor],
  hasShields: Boolean,
  hullState: Seq[Byte],
  isBuilding: Boolean,
  animationTime: Int,
  playerColor: ColorRGB
) extends CompositeModelBuilder[DroneModel, DroneModelParameters]
    with WorldObjectDescriptor[DroneModelParameters] {

  require(hullState.size == sides - 1)

  override protected def buildSubcomponents
    : (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, DroneModelParameters]]) = {
    val colorPalette =
      if (signature.isBuilding) MutedDroneColors
      else DefaultDroneColors
    val playerColor =
      if (isBuilding) signature.playerColor * 0.5f
      else signature.playerColor
    import colorPalette._

    val sideLength = 40
    val radiusBody = 0.5f * sideLength / sin(Pi / sides).toFloat
    val radiusHull = radiusBody + circumradius(4, sides)

    val body =
      Polygon(
        rs.MaterialXYZRGB,
        sides,
        ColorBody,
        ColorBody,
        radius = radiusBody
      )

    val hullColors = hullState.map {
      case 2 => ColorHull
      case 1 => ColorHullDamaged
      case 0 => ColorHullBroken
    }
    val hull =
      PolygonRing(
        rs.MaterialXYZRGB,
        sides,
        playerColor +: hullColors,
        playerColor +: hullColors,
        radiusBody,
        radiusHull,
        NullVectorXY,
        0,
        0
      )

    val modulesModel =
      for {
        module <- modules
      } yield
        module match {
          case EnginesDescriptor(position) =>
            DroneEnginesModel(ModulePosition(sides, position), colorPalette, playerColor, animationTime)(rs)
          case MissileBatteryDescriptor(position, n) =>
            DroneMissileBatteryModel(colorPalette, playerColor, ModulePosition(sides, position), n)(rs)
          case LongRangeMissileBatteryDescriptor(position, chargeup) =>
            DroneLongRangeMissileBatteryModel(colorPalette,
                                              playerColor,
                                              ModulePosition(sides, position),
                                              chargeup)(rs)
          case ShieldGeneratorDescriptor(position) =>
            DroneShieldGeneratorModel(ModulePosition(sides, position), colorPalette, playerColor)(rs)
          case StorageModuleDescriptor(position, contents) =>
            DroneStorageModel(ModulePosition(sides, position), colorPalette, contents)(rs)
          case ManipulatorDescriptor(position) =>
            DroneConstructorModel(colorPalette, playerColor, ModulePosition(sides, position))(rs)
        }

    val staticModels = body +: hull +: modulesModel

    var dynamicModels = Seq.empty[ModelBuilder[_, DroneModelParameters]]
    if (hasShields) {
      dynamicModels :+=
        PolygonRing(
          material = rs.TranslucentAdditivePIntensity,
          n = 50,
          colorInside = ColorRGBA(ColorThrusters, 0f),
          colorOutside = ColorRGBA(White, 0.7f),
          outerRadius = radiusHull + 2,
          innerRadius = Geometry.inradius(radiusHull, sides) * 0.85f
        ).wireParameters[DroneModelParameters](d => Intensity(d.shieldState.get))
    }

    // TODO: make this work again
    /*
    if (!isBuilding) {
      dynamicModels :+=
        new DynamicModel(
          new DroneThrusterTrailsModelFactory(
            sideLength, radiusHull, sides, playerColor
          ).buildModel
        ).wireParameters[DroneDescriptor](d => d.positions)
    }*/

    (staticModels, dynamicModels)
  }

  override protected def decorate(
    model: Model[DroneModelParameters],
    context: GraphicsContext
  ): Model[DroneModelParameters] =
    if (signature.isBuilding)
      model
        .translated(VertexXYZ(0, 0, -3), context.useTransposedModelview)
        .withDynamicVertexCount
        .wireParameters[DroneModelParameters](d => (d.constructionState.get, d))
    else model

  def signature = this
  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle) =
    intersects(xPos, yPos, 100, rectangle) // FIXME
}

private[codecraft] sealed trait DroneModuleDescriptor

private[codecraft] case class EnginesDescriptor(position: Int) extends DroneModuleDescriptor
private[codecraft] case class ShieldGeneratorDescriptor(position: Int) extends DroneModuleDescriptor
private[codecraft] case class MissileBatteryDescriptor(position: Int, n: Int = 3)
    extends DroneModuleDescriptor
private[codecraft] case class LongRangeMissileBatteryDescriptor(position: Int, chargeup: Int)
    extends DroneModuleDescriptor
private[codecraft] case class ManipulatorDescriptor(position: Int) extends DroneModuleDescriptor
private[codecraft] case class StorageModuleDescriptor(
  position: Int,
  contents: StorageModuleContents
) extends DroneModuleDescriptor

private[codecraft] sealed trait StorageModuleContents
private[codecraft] case object EmptyStorage extends StorageModuleContents
private[codecraft] case object MineralStorage extends StorageModuleContents
private[codecraft] case class EnergyStorage(filledPositions: Set[Int] = Set(0, 1, 2, 3, 4, 5, 6))
    extends StorageModuleContents

private[codecraft] case class DroneModelParameters(
  shieldState: Option[Float],
  constructionState: Option[Float0To1] = None
)

private[graphics] trait DroneColors {
  val Black: ColorRGB
  val White: ColorRGB
  val ColorBody: ColorRGB
  val ColorHull: ColorRGB
  val ColorHullDamaged: ColorRGB
  val ColorHullBroken: ColorRGB
  val ColorThrusters: ColorRGB
  val ColorBackplane: ColorRGB
}

private[graphics] object DefaultDroneColors extends DroneColors {
  final val Black = ColorRGB(0, 0, 0)
  final val White = ColorRGB(1, 1, 1)
  final val ColorBody = Black
  final val ColorHull = ColorRGB(0.95f, 0.95f, 0.95f)
  final val ColorHullDamaged = ColorRGB(0.6f, 0.6f, 0.6f)
  final val ColorHullBroken = ColorRGB(0.2f, 0.2f, 0.2f)
  final val ColorThrusters = ColorRGB(0, 0, 1)
  final val ColorBackplane = ColorRGB(0.1f, 0.1f, 0.1f)
}

private[graphics] object MutedDroneColors extends DroneColors {
  final val dimmingFactor = 0.5f
  final val Black = DefaultDroneColors.Black * dimmingFactor
  final val White = DefaultDroneColors.White * dimmingFactor
  final val ColorBody = DefaultDroneColors.ColorBody * dimmingFactor
  final val ColorHull = DefaultDroneColors.ColorHull * dimmingFactor
  final val ColorHullDamaged = DefaultDroneColors.ColorHullDamaged * dimmingFactor
  final val ColorHullBroken = DefaultDroneColors.ColorHullBroken * dimmingFactor
  final val ColorThrusters = DefaultDroneColors.ColorThrusters * dimmingFactor
  final val ColorBackplane = DefaultDroneColors.ColorBackplane * dimmingFactor
}
