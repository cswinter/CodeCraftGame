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
  //noinspection ZeroIndexToHead
  val ModulePosition = Map[Int, IndexedSeq[VertexXY]](
    3 -> IndexedSeq(VertexXY(0, 0)),

    4 -> IndexedSeq(VertexXY(9, 9), VertexXY(-9, -9)),

    5 -> Geometry.polygonVertices2(4, radius = 17),

    6 -> (Geometry.polygonVertices2(6, radius = 27) :+ NullVectorXY),

    7 -> (Geometry.polygonVertices2(7, radius = 33) ++
      Geometry.polygonVertices(3, orientation = math.Pi.toFloat, radius = 13))
  )
}


case class RobotSignature(
  size: Int,
  modules: Seq[RobotModule],
  hasShields: Boolean,
  hullState: Seq[Byte],
  constructionState: Int
)

object RobotSignature {
  def apply(robotObject: RobotObject): RobotSignature = {
    RobotSignature(
      robotObject.size,
      robotObject.modules,
      robotObject.modules.contains(ShieldGenerator),
      robotObject.hullState,
      robotObject.constructionState)
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

    val modules =
      for {
        (module, index) <- signature.modules.zipWithIndex
        position = ModulePosition(sides)(index)
      } yield (module match {
        case Engines(t) => RobotEnginesModel(position, t)
        case ProcessingModule(t) => FactoryModelBuilder(position, t)
        case StorageModule(count) => RobotStorageModelBuilder(position, count)
        case Lasers(n) => RobotLasersModelBuilder(position, n)
        case ShieldGenerator => RobotShieldGeneratorModel(position)
      }).getModel

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
      if (signature.constructionState == -1) {
        new DynamicModel(
          new RobotThrusterTrailsModelFactory(
            sideLength, radiusHull, sides).buildModel)
      } else new EmptyModel[Seq[(Float, Float, Float)]]

    val other = Seq()

    new RobotModel(body, hull, modules, shields, thrusters, other)
  }
}


case class RobotModel(
  body: Model[Unit],
  hull: Model[Unit],
  modules: Seq[Model[Unit]],
  shields: Option[Model[Unit]],
  thrusterTrails: Model[Seq[(Float, Float, Float)]],
  other: Seq[Model[Unit]]
) extends CompositeModel[RobotObject] {

  // MAKE SURE TO ADD NEW COMPONENTS HERE:
  val models: Seq[Model[_]] =
    Seq(body, hull, thrusterTrails) ++ modules ++ shields.toSeq ++ other

  override def update(a: RobotObject): Unit = {
    thrusterTrails.update(a.positions)

    if (a.constructionState != -1) {
      val flicker = (a.constructionState % 5 & 1) ^ 1
      setVertexCount((a.constructionState / 5 + flicker) * 3)
    }
  }
}

