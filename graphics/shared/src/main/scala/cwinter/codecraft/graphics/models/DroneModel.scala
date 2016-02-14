package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.materials.Intensity
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths._
import cwinter.codecraft.util.maths.matrices.{Matrix4x4, TranslationMatrix4x4}


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
  final val ColorBody = ColorRGB(0.05f, 0.05f, 0.05f)
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


private[graphics] case class DroneSignature(
  size: Int,
  modules: Seq[DroneModuleDescriptor],
  hasShields: Boolean,
  hullState: Seq[Byte],
  isBuilding: Boolean,
  animationTime: Int,
  playerColor: ColorRGB
)

private[graphics] object DroneSignature {
  def apply(droneObject: DroneDescriptor, timestep: Int): DroneSignature = {
    val hasAnimatedComponents = droneObject.modules.exists(m =>
      m.isInstanceOf[EnginesDescriptor] || m.isInstanceOf[ProcessingModuleDescriptor]
    )
    val isBuilding = droneObject.constructionState.isDefined
    DroneSignature(
      droneObject.size,
      droneObject.modules,
      droneObject.modules.exists(_.isInstanceOf[ShieldGeneratorDescriptor]),
      droneObject.hullState,
      isBuilding,
      if (hasAnimatedComponents && !isBuilding) timestep % 100 else 0,
      droneObject.playerColor)
  }
}


private[graphics] class DroneModelBuilder(
  drone: DroneDescriptor,
  timestep: Int
)(implicit val rs: RenderStack) extends ModelBuilder[DroneSignature, DroneDescriptor] {

  val signature: DroneSignature = DroneSignature(drone, timestep)

  import Geometry.circumradius
  import cwinter.codecraft.util.modules.ModulePosition

  import scala.math._

  protected def buildModel: Model[DroneDescriptor] = {
    import signature._
    val colorPalette =
      if (signature.isBuilding) MutedDroneColors
      else DefaultDroneColors
    val playerColor =
      if (isBuilding) signature.playerColor * 0.5f
      else signature.playerColor
    import colorPalette._

    val sides = drone.size
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
      ).getModel

    val hullColors = drone.hullState.map {
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
      ).getModel

    val modulesModel =
      for {
        module <- modules
      } yield (module match {
        case EnginesDescriptor(position) =>
          DroneEnginesModel(ModulePosition(sides, position), colorPalette, playerColor, animationTime)
        case MissileBatteryDescriptor(position, n) =>
          DroneMissileBatteryModelBuilder(colorPalette, playerColor, ModulePosition(sides, position), n)
        case ShieldGeneratorDescriptor(position) =>
          DroneShieldGeneratorModel(ModulePosition(sides, position), colorPalette, playerColor)
        case ProcessingModuleDescriptor(positions, tMerging) =>
          ProcessingModuleModelBuilder(ModulePosition(sides, positions), animationTime, tMerging, positions.size)
        case StorageModuleDescriptor(position, contents, mineralPosition) =>
          DroneStorageModelBuilder(ModulePosition(sides, position), colorPalette, contents, mineralPosition)
        case ManipulatorDescriptor(position, constructionPos, active) =>
          DroneManipulatorModelBuilder(colorPalette, playerColor, ModulePosition(sides, position), constructionPos, active)
      }).getModel

    val shields =
      if (hasShields) {
        Some(PolygonRing(
          material = rs.TranslucentAdditivePIntensity,
          n = 50,
          colorInside = ColorRGBA(ColorThrusters, 0f),
          colorOutside = ColorRGBA(White, 0.7f),
          outerRadius = radiusHull + 2,
          innerRadius = Geometry.inradius(radiusHull, sides) * 0.85f
        ).getModel)
      } else None

    val thrusters =
      if (!isBuilding) {
        new DynamicModel(
          new DroneThrusterTrailsModelFactory(
            sideLength, radiusHull, sides, playerColor).buildModel)
      } else new EmptyModel[Seq[(Float, Float, Float)]]


    val other = {
      for (r <- drone.sightRadius)
        yield PolygonRing(
          material = rs.MaterialXYZRGB,
          n = 50,
          colorInside = White,
          colorOutside = White,
          innerRadius = r,
          outerRadius = r + 1,
          zPos = 2
        ).getModel
    }.toSeq



    new DroneModel(body, hull, modulesModel, shields, thrusters, other, new ImmediateModeModel, rs)
  }
}


private[graphics] case class DroneModel(
  body: Model[Unit],
  hull: Model[Unit],
  modules: Seq[Model[Unit]],
  shields: Option[Model[Intensity]],
  thrusterTrails: Model[Seq[(Float, Float, Float)]],
  other: Seq[Model[Unit]],
  immediateMode: ImmediateModeModel,
  rs: RenderStack
) extends CompositeModel[DroneDescriptor] {
  private[this] var constructionState: Option[Int] = None

  // MAKE SURE TO ADD NEW COMPONENTS HERE:
  val models: Seq[Model[_]] =
    Seq(body, hull, thrusterTrails, immediateMode) ++ modules ++ shields.toSeq ++ other

  override def update(a: DroneDescriptor): Unit = {
    thrusterTrails.update(a.positions)

    constructionState = a.constructionState.map(f => (f * vertexCount / 3).toInt)

    shields.foreach(_.update(Intensity(a.shieldState.getOrElse(0))))
  }

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = {
    for (t <- constructionState) setVertexCount(t * 3)

    // TODO: rs.modelviewTranspose is brittle (because of code like this)
    val modelview2 =
      if (constructionState.isDefined) {
        if (rs.modelviewTranspose) modelview * new TranslationMatrix4x4(0, 0, -1).transposed
        else new TranslationMatrix4x4(0, 0, -1) * modelview
      } else modelview


    super.draw(modelview2, material)
    // we need to restore vertex count, since models for submodules may be shared with other drones
    if (constructionState.nonEmpty) setVertexCount(Integer.MAX_VALUE)
  }
}

