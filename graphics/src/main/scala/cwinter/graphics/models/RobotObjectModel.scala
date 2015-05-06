package cwinter.graphics.models

import cwinter.codinggame.util.maths._
import cwinter.graphics.engine.RenderStack
import cwinter.graphics.matrices.{TranslationMatrix4x4, DilationXYMatrix4x4, Matrix4x4}
import cwinter.graphics.model._
import cwinter.worldstate._
import cwinter.graphics.models.RobotColors._


object RobotColors {
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
  modules: Seq[DroneModule],
  hasShields: Boolean,
  hullState: Seq[Byte],
  isBuilding: Boolean,
  animationTime: Int,
  player: Player
)

object DroneSignature {
  def apply(robotObject: DroneDescriptor, timestep: Int): DroneSignature = {
    DroneSignature(
      robotObject.size,
      robotObject.modules,
      robotObject.modules.exists(_.isInstanceOf[ShieldGenerator]),
      robotObject.hullState,
      robotObject.constructionState != None,
      timestep % 100,
      robotObject.player)
  }
}


class DroneModelBuilder(robot: DroneDescriptor, timestep: Int)(implicit val rs: RenderStack)
  extends ModelBuilder[DroneSignature, DroneDescriptor] {
  def signature: DroneSignature = DroneSignature(robot, timestep)

  import Geometry.circumradius
  import cwinter.codinggame.util.modules.ModulePosition

  import scala.math._

  protected def buildModel: Model[DroneDescriptor] = {
    val sides = robot.size
    val sideLength = 40
    val radiusBody = 0.5f * sideLength / sin(Pi / sides).toFloat
    val radiusHull = radiusBody + circumradius(4, sides)


    val body =
      Polygon(
        rs.MaterialXYRGB,
        sides,
        ColorBody,
        ColorBody,
        radius = radiusBody
      ).getModel

    val hullColors = robot.hullState.map {
      case 2 => ColorHull
      case 1 => ColorHullDamaged
      case 0 => ColorHullBroken
    }
    val hull =
      PolygonRing(
        rs.MaterialXYRGB,
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
        case Engines(position) =>
          RobotEnginesModel(ModulePosition(sides, position), signature.animationTime)
        case Lasers(position, n) =>
          RobotLasersModelBuilder(signature.player, ModulePosition(sides, position), n)
        case ShieldGenerator(position) =>
          RobotShieldGeneratorModel(ModulePosition(sides, position))
        case ProcessingModule(positions, tMerging) =>
          FactoryModelBuilder(ModulePosition(sides, positions), signature.animationTime, tMerging, positions.size)
        case StorageModule(positions, count, tm) =>
          RobotStorageModelBuilder(ModulePosition(sides, positions), count, positions.size, tm)
        case Manipulator(position) =>
          DroneManipulatorModelBuilder(signature.player, ModulePosition(sides, position))
      }).getModel

    val shields =
      if (signature.hasShields)
        Some(PolygonRing(
          material = rs.TranslucentAdditive,
          n = 50,
          colorInside = ColorRGBA(ColorThrusters, 0f),
          colorOutside = ColorRGBA(White, 0.7f),
          outerRadius = radiusHull + 2,
          innerRadius = Geometry.inradius(radiusHull, sides) * 0.85f
        ).getModel)
      else None

    val thrusters =
      if (!signature.isBuilding) {
        new DynamicModel(
          new RobotThrusterTrailsModelFactory(
            sideLength, radiusHull, sides).buildModel)
      } else new EmptyModel[Seq[(Float, Float, Float)]]


    val other = {
      for (r <- robot.sightRadius)
        yield PolygonRing(
          material = rs.MaterialXYRGB,
          n = 50,
          colorInside = White,
          colorOutside = White,
          innerRadius = r,
          outerRadius = r + 1,
          zPos = 2
        ).getModel
    }.toSeq



    new RobotModel(body, hull, modules, shields, thrusters, other, new ImmediateModeModel, rs)
  }
}


case class RobotModel(
  body: Model[Unit],
  hull: Model[Unit],
  modules: Seq[Model[Unit]],
  shields: Option[Model[Unit]],
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

    constructionState = a.constructionState

    val sightLines: Iterable[Model[Unit]] =
      for {
        inSight <- a.inSight.toIterable
        (x, y) <- inSight
      } yield new QuadStrip(
        rs.MaterialXYRGB,
        Seq(VertexXY(x, y), VertexXY(a.xPos, a.yPos)),
        Seq(ColorThrusters, ColorThrusters),
        width = 1,
        zPos = 2
      ).noCaching.getModel.identityModelview
    immediateMode.update(sightLines.toSeq)
  }

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = {
    for {
      t <- constructionState
      flicker = (t % 5 & 1) ^ 1
    } setVertexCount((t / 5 + flicker) * 3)

    val modelview2 =
      if (constructionState.isDefined) {
        new TranslationMatrix4x4(0, 0, -1) * modelview
      } else modelview

    super.draw(modelview2, material)
    setVertexCount(Integer.MAX_VALUE)
  }
}

