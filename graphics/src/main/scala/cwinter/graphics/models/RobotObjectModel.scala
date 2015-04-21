package cwinter.graphics.models

import cwinter.graphics.engine.RenderStack
import cwinter.graphics.matrices.Matrix4x4
import cwinter.graphics.model._
import cwinter.worldstate._
import scala.math._
import cwinter.graphics.models.RobotColors._
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

    6 -> permutation(
      Geometry.polygonVertices2(6, radius = 27) :+ NullVectorXY,
      IndexedSeq(1, 0, 5, 6, 4, 3, 2)
    ),

    7 -> permutation(Geometry.polygonVertices2(7, radius = 33) ++
      Geometry.polygonVertices(3, orientation = math.Pi.toFloat, radius = 13),
      IndexedSeq(0, 1, 9, 2, 3, 7, 4, 5, 8, 6)
    )
  )


  private def permutation[T](set: IndexedSeq[T], indices: IndexedSeq[Int]): IndexedSeq[T] = {
    IndexedSeq.tabulate(set.size)(i => set(indices(i)))
  }
}


case class RobotSignature(
  size: Int,
  modules: Seq[DroneModule],
  hasShields: Boolean,
  hullState: Seq[Byte],
  isBuilding: Boolean
)

object RobotSignature {
  def apply(robotObject: DroneDescriptor): RobotSignature = {
    RobotSignature(
      robotObject.size,
      robotObject.modules,
      robotObject.modules.exists(_.isInstanceOf[ShieldGenerator]),
      robotObject.hullState,
      robotObject.constructionState != None)
  }
}


class RobotModelBuilder(robot: DroneDescriptor)(implicit val rs: RenderStack)
  extends ModelBuilder[RobotSignature, DroneDescriptor] {
  def signature: RobotSignature = RobotSignature(robot)

  import Geometry.circumradius
  import RobotModulePositions.ModulePosition

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


    @inline
    def pos(positions: Seq[Int]): VertexXY =
      positions.map(ModulePosition(sides)(_)).reduce(_ + _) / positions.size

    val modules =
      for {
        module <- signature.modules
      } yield (module match {
        case Engines(position, t) =>
          RobotEnginesModel(ModulePosition(sides)(position), t)
        case Lasers(position, n) =>
          RobotLasersModelBuilder(ModulePosition(sides)(position), n)
        case ShieldGenerator(position) =>
          RobotShieldGeneratorModel(ModulePosition(sides)(position))
        case ProcessingModule(positions, t, t2) =>
          FactoryModelBuilder(positions.map(ModulePosition(sides)(_)), t, t2, positions.size)
        case StorageModule(positions, count, tm) =>
          RobotStorageModelBuilder(positions.map(ModulePosition(sides)(_)), count, positions.size, tm)
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
    super.draw(modelview, material)
    setVertexCount(Integer.MAX_VALUE)
  }
}

