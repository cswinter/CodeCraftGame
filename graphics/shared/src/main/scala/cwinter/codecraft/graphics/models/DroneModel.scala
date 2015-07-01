package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.materials.Intensity
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.models.DroneColors._
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths._
import cwinter.codecraft.util.maths.matrices.{Matrix4x4, TranslationMatrix4x4}


object DroneColors {
  val Black = ColorRGB(0, 0, 0)
  val White = ColorRGB(1, 1, 1)
  val ColorBody = ColorRGB(0.05f, 0.05f, 0.05f)
  val ColorHull = ColorRGB(0.95f, 0.95f, 0.95f)
  val ColorHullDamaged = ColorRGB(0.6f, 0.6f, 0.6f)
  val ColorHullBroken = ColorRGB(0.2f, 0.2f, 0.2f)
  val ColorThrusters = ColorRGB(0, 0, 1)
  val ColorBackplane = ColorRGB(0.1f, 0.1f, 0.1f)
}


case class DroneSignature(
  size: Int,
  modules: Seq[DroneModuleDescriptor],
  hasShields: Boolean,
  hullState: Seq[Byte],
  isBuilding: Boolean,
  animationTime: Int,
  player: Player
)

object DroneSignature {
  def apply(droneObject: DroneDescriptor, timestep: Int): DroneSignature = {
    val hasAnimatedComponents = droneObject.modules.exists(m =>
      m.isInstanceOf[EnginesDescriptor] || m.isInstanceOf[ProcessingModuleDescriptor]
    )
    DroneSignature(
      droneObject.size,
      droneObject.modules,
      droneObject.modules.exists(_.isInstanceOf[ShieldGeneratorDescriptor]),
      droneObject.hullState,
      droneObject.constructionState != None,
      if (hasAnimatedComponents) timestep % 100 else 0,
      droneObject.player)
  }
}


class DroneModelBuilder(drone: DroneDescriptor, timestep: Int)(implicit val rs: RenderStack)
  extends ModelBuilder[DroneSignature, DroneDescriptor] {
  def signature: DroneSignature = DroneSignature(drone, timestep)

  import Geometry.circumradius
  import cwinter.codecraft.util.modules.ModulePosition

  import scala.math._

  protected def buildModel: Model[DroneDescriptor] = {
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
        signature.player.color +: hullColors,
        signature.player.color +: hullColors,
        radiusBody,
        radiusHull,
        NullVectorXY,
        0,
        0
      ).getModel

    val modules =
      for {
        module <- signature.modules
      } yield (module match {
        case EnginesDescriptor(position) =>
          DroneEnginesModel(ModulePosition(sides, position), signature.player, signature.animationTime)
        case MissileBatteryDescriptor(position, n) =>
          DroneMissileBatteryModelBuilder(signature.player, ModulePosition(sides, position), n)
        case ShieldGeneratorDescriptor(position) =>
          DroneShieldGeneratorModel(ModulePosition(sides, position), signature.player)
        case ProcessingModuleDescriptor(positions, tMerging) =>
          ProcessingModuleModelBuilder(ModulePosition(sides, positions), signature.animationTime, tMerging, positions.size)
        case StorageModuleDescriptor(positions, contents, tm) =>
          DroneStorageModelBuilder(ModulePosition(sides, positions), contents, positions.size, tm)
        case ManipulatorDescriptor(position) =>
          DroneManipulatorModelBuilder(signature.player, ModulePosition(sides, position))
      }).getModel

    val shields =
      if (signature.hasShields) {
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
      if (!signature.isBuilding) {
        new DynamicModel(
          new DroneThrusterTrailsModelFactory(
            sideLength, radiusHull, sides, signature.player).buildModel)
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



    new DroneModel(body, hull, modules, shields, thrusters, other, new ImmediateModeModel, rs)
  }
}


case class DroneModel(
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

    val sightLines: Iterable[Model[Unit]] =
      for {
        inSight <- a.inSight.toIterable
        (x, y) <- inSight
      } yield new QuadStrip(
        rs.MaterialXYZRGB,
        Seq(VertexXY(x, y), VertexXY(a.xPos, a.yPos)),
        Seq(ColorThrusters, ColorThrusters),
        width = 1,
        zPos = 2
      ).noCaching.getModel.identityModelview
    immediateMode.update(sightLines.toSeq)
  }

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = {
    for (t <- constructionState) setVertexCount(t * 3)

    val modelview2 =
      if (constructionState.isDefined) {
        new TranslationMatrix4x4(0, 0, -1) * modelview
      } else modelview


    super.draw(modelview2, material)
    setVertexCount(Integer.MAX_VALUE)
  }
}

