package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import robowars.worldstate._
import scala.math._
import robowars.graphics.models.RobotColors._
import Geometry._


object RobotColors {
  val Black = ColorRGB(0, 0, 0)
  val White = ColorRGB(1, 1, 1)
  val ColorBody = ColorRGB(0.05f, 0.05f, 0.05f)
  val ColorHull = ColorRGB(0.95f, 0.95f, 0.95f)
  val ColorHullDamaged = ColorRGB(0.5f, 0.5f, 0.5f)
  val ColorHullBroken = Black
  val ColorThrusters = ColorRGB(0, 0, 1)
  val ColorBackplane = ColorRGB(0.1f, 0.1f, 0.1f)
}


object RobotModulePositions {
  val hexRad = 27.0f
  val hexInRad = 11.0f
  val hexagonVertices = Geometry.polygonVertices(6, Pi.toFloat / 6, hexRad)
  val ModulePosition = Map[(Int, Int), VertexXY](
    (3, 0) -> VertexXY(0, 0),

    (4, 0) -> VertexXY(9, 4),
    (4, 1) -> VertexXY(-9, 9),
    (4, 2) -> VertexXY(-4, -9),

    (5, 0) -> VertexXY(-17, 11),
    (5, 1) -> VertexXY(-17, -11),
    (5, 2) -> VertexXY(6, 20),
    (5, 3) -> VertexXY(0, 0),
    (5, 4) -> VertexXY(6, -20),
    (5, 5) -> VertexXY(20, 0),

    (6, 0) -> hexagonVertices(0),
    (6, 1) -> hexagonVertices(1),
    (6, 2) -> hexagonVertices(2),
    (6, 3) -> hexagonVertices(3),
    (6, 4) -> hexagonVertices(4),
    (6, 5) -> hexagonVertices(5),

    (6, 6) -> hexInRad * VertexXY(0 * 2 * Pi.toFloat / 3),
    (6, 7) -> hexInRad * VertexXY(1 * 2 * Pi.toFloat / 3),
    (6, 8) -> hexInRad * VertexXY(2 * 2 * Pi.toFloat / 3)
  )
}


case class RobotSignature(
  size: Int,
  engines: Seq[(Engines, Int)],
  factories: Seq[(ProcessingModule, Int)],
  storageModules: Seq[(StorageModule, Int)],
  shieldGeneratorModels: Seq[(ShieldGenerator.type, Int)],
  hasShields: Boolean,
  hullState: Seq[Byte]
)

object RobotSignature {
  def apply(robotObject: RobotObject): RobotSignature = {
    val engines =
      for ((engines: Engines, index) <- robotObject.modules.zipWithIndex)
      yield (engines, index)

    val factories =
      for ((factory: ProcessingModule, index) <- robotObject.modules.zipWithIndex)
        yield (factory, index)

    val storageModules =
      for ((storage: StorageModule, index) <- robotObject.modules.zipWithIndex)
      yield (storage, index)

    val shieldGeneratorModels =
      for ((ShieldGenerator, index) <- robotObject.modules.zipWithIndex)
      yield (ShieldGenerator, index)

    RobotSignature(
      robotObject.size,
      engines,
      factories,
      storageModules,
      shieldGeneratorModels,
      shieldGeneratorModels.nonEmpty,
      robotObject.hullState)
  }
}


class RobotModelBuilder(robot: RobotObject)(implicit val rs: RenderStack)
  extends ModelBuilder[RobotSignature, RobotObject] {
  def signature: RobotSignature = RobotSignature(robot)

  import Geometry.circumradius
  import RobotModulePositions.ModulePosition

  import scala.math._

  protected def buildModel: Model[RobotObject] = {
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

    val hullColors = ColorBackplane +: robot.hullState.map {
      case 2 => ColorHull
      case 1 => ColorHullDamaged
      case 0 => ColorHullBroken
    }
    val hull =
      PolygonRing(
        rs.MaterialXYRGB,
        sides,
        hullColors,
        hullColors,
        radiusBody,
        radiusHull,
        NullVectorXY,
        0,
        0
      ).getModel

    val shields =
      if (signature.hasShields)
        Some(Polygon(
          material = rs.TranslucentAdditive,
          n = 50,
          colorMidpoint = ColorRGBA(ColorThrusters, 0.1f),
          colorOutside = ColorRGBA(White, 0.5f),
          radius = radiusHull + 5
        ).getModel)
      else None

    val thrusters =
      new DynamicModel(
        new RobotThrusterTrailsModelFactory(
          sideLength, radiusHull, sides).buildModel)

    val engines =
      for ((Engines(t), index) <- signature.engines)
      yield RobotEnginesModel(ModulePosition((sides, index)), t).getModel

    val factories =
      for ((ProcessingModule(t), index) <- signature.factories)
        yield FactoryModelBuilder(ModulePosition((sides, index)), t % 250).getModel

    val storageModules =
      for ((StorageModule(count), index) <- signature.storageModules)
      yield RobotStorageModule(ModulePosition((sides, index)), count).getModel

    val shieldGeneratorModules =
      for ((ShieldGenerator, index) <- signature.shieldGeneratorModels)
      yield RobotShieldGeneratorModel(ModulePosition((sides, index))).getModel

    new RobotModel(body, hull, engines, factories, storageModules, shieldGeneratorModules, shields, thrusters)
  }


}


case class RobotModel(
  body: Model[Unit],
  hull: Model[Unit],
  engines: Seq[Model[Unit]],
  factories: Seq[Model[Unit]],
  storageModules: Seq[Model[Unit]],
  shieldGeneratorModules: Seq[Model[Unit]],
  shields: Option[Model[Unit]],
  thrusterTrails: Model[Seq[(Float, Float, Float)]]
) extends CompositeModel[RobotObject] {

  // MAKE SURE TO ADD NEW COMPONENTS HERE:
  val models: Seq[Model[_]] =
    Seq(body, hull, thrusterTrails) ++ engines ++ factories ++ storageModules ++ shieldGeneratorModules ++ shields.toSeq

  override def update(a: RobotObject): Unit = {
    thrusterTrails.update(a.positions)
  }
}









