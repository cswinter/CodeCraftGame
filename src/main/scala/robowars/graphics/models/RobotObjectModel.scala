package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.matrices.IdentityMatrix4x4
import robowars.graphics.model._
import robowars.graphics.primitives._
import robowars.worldstate._
import scala.math._


class RobotObjectModel(robot: RobotObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(robot) {



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

  val ModuleCount: Map[Int, Int] = {
    for ((size, _) <- ModulePosition.keys) yield size -> ModulePosition.count(_._1._1 == size)
  }.toMap

  val sides = robot.size
  val sideLength = 40
  val radiusBody = 0.5f * sideLength / sin(Pi / sides).toFloat
  val radiusHull = radiusBody + circumradius(4)


  val ColorBody = ColorRGB(0.05f, 0.05f, 0.05f)
  val ColorHull = ColorRGB(0.95f, 0.95f, 0.95f)
  val ColorThrusters = if (robot.identifier % 2 == 0) ColorRGB(0, 0, 1) else ColorRGB(1, 0, 0)
  val ColorBackplane = ColorRGB(0.1f, 0.1f, 0.1f)
  val Black = ColorRGB(0, 0, 0)
  val White = ColorRGB(1, 1, 1)


  def storageModule(position: VertexXY, nEnergy: Int = 0): ComposableModel = {
    val radius = 8
    val outlineWidth = 1
    val container = Seq(
      new PolygonOld(20, renderStack.MaterialXYRGB)
        .scale(radius - outlineWidth)
        .color(ColorBackplane)
        .zPos(1)
        .translate(position),
      new NewPolygonOutline(renderStack.MaterialXYRGB)(20, radius - outlineWidth, radius)
        .color(ColorHull)
        .zPos(1)
        .translate(position)
    )

    val energyPositions = Seq(VertexXY(0, 0)) ++ Geometry.polygonVertices(6, radius = 4.5f)
    val energyGlobes =
      for (i <- 0 until nEnergy)
      yield new PolygonOld(7, renderStack.BloomShader)
        .scale(2)
        .translate(energyPositions(i) + position)
        .color(ColorRGB(0, 1, 0))
        .colorMidpoint(ColorRGB(1, 1, 1))
        .zPos(2)

    (container ++ energyGlobes).reduce[ComposableModel](_ + _)
  }


  def shieldGeneratorModule(position: VertexXY): ComposableModel = {
    val radius = 3
    val gridpointRadius = 2 * inradius(radius, 6)
    val gridpoints = VertexXY(0, 0) +: Geometry.polygonVertices(6, radius = gridpointRadius)
    val hexagons =
      for (pos <- gridpoints)
      yield new NewPolygonOutline(renderStack.MaterialXYRGB)(6, radius - 0.5f, radius)
        .translate(pos + position)
        .color(White)
        .zPos(1)

    val filling =
      for (pos <- gridpoints)
      yield new PolygonOld(6, renderStack.MaterialXYRGB)
        .scale(radius - 0.5f)
        .translate(pos + position)
        .color(ColorThrusters)
        .zPos(1)

    (hexagons ++ filling).reduce[ComposableModel](_ + _)
  }

  def thruster(side: Int) = {
    new RichCircleSegment(8, 0.7f, renderStack.MaterialXYRGB)
      .scaleX(5)
      .scaleY(sideLength * 0.25f)
      .rotate(Pi.toFloat)
      .translate(computeThrusterPos(side))
      .colorMidpoint(ColorThrusters)
      .colorOutside(ColorBackplane)
      .zPos(1)
  }

  def generateThrusterTrails(positions: Seq[(Float, Float, Float)]): ComposableModel = {
    val n = positions.length
    val trailPositions =
      for (((x, y, a), t) <- positions.zipWithIndex)
      yield {
        val drift = -VertexXY(a) * (n - t - 1) * 2.0f
        val offset = VertexXY(x, y) + drift
        (computeThrusterPos(1, a) + offset, computeThrusterPos(-1, a) + offset)
      }

    val (trail1, trail2) = trailPositions.unzip
    val colors = trail1.indices.map(index => ColorRGBA(ColorThrusters, index / n.toFloat))

    new QuadStrip(sideLength * 0.3f, trail1)(renderStack.TranslucentAdditive)
      .colorMidpoints(colors) +
      new QuadStrip(sideLength * 0.3f, trail2)(renderStack.TranslucentAdditive)
        .colorMidpoints(colors)
  }

  def computeThrusterPos(side: Int, angle: Float = 0): VertexXY = {
    val perp = outerModulePerpendicular(0, angle)
    outerModulePosition(0, angle) + side * sideLength * 0.3f * perp
  }


  val modelComponents = Seq(
    /* body */
    new PolygonOld(sides, renderStack.MaterialXYRGB)
      .scale(radiusBody)
      .color(ColorBody),

    /* hull */
    new NewPolygonOutline(renderStack.MaterialXYRGB)(sides, radiusBody, radiusHull)
      .color(ColorHull)
      .colorSide(ColorBackplane, sides - 1),

    /* thrusters */
    thruster(1),
    thruster(-1)
  )

  val shield =
    if (robot.modules.contains(ShieldGenerator)) {
      new PolygonOld(50, renderStack.TranslucentAdditive)
        .scale(radiusHull + 5)
        .colorOutside(ColorRGBA(White, 0.5f))
        .colorMidpoint(ColorRGBA(ColorThrusters, 0.1f))
    } else EmptyModel

  val modules =
    if (ModuleCount.contains(sides)) {
      for {
        (m, i) <- robot.modules.zipWithIndex
        pos = ModulePosition((sides, i))
      } yield m match {
        case StorageModule(r) => storageModule(pos, r)
        case ShieldGenerator | Engines => shieldGeneratorModule(pos)
      }
    } else Seq()


  val thrusterTrails = new MutableWrapperModel(generateThrusterTrails(robot.positions).init())

  val staticModels = (modelComponents ++ modules :+ shield).reduce[ComposableModel]((x, y) => x + y)
  val model = staticModels.init() * thrusterTrails


  override def update(worldObject: WorldObject): this.type = {
    val robotObject = worldObject.asInstanceOf[RobotObject]

    thrusterTrails.replaceModel(generateThrusterTrails(robotObject.positions).init())
    super.update(worldObject)
    thrusterTrails.setModelview(IdentityMatrix4x4)
    this
  }

  def outerModulePosition(n: Int, orientationOffset: Float = 0): VertexXY = {
    assert(sides > n)
    assert(n >= 0)
    val r = inradius(radiusHull)
    r * outerModuleNormal(n, orientationOffset)
  }

  def outerModuleNormal(n: Int, orientationOffset: Float = 0): VertexXY = {
    val angle = Pi + (2 * n * Pi / sides) + orientationOffset
    VertexXY(angle)
  }

  def outerModulePerpendicular(n: Int, orientationOffset: Float = 0): VertexXY = {
    outerModuleNormal(n, orientationOffset).perpendicular
  }


  /**
   * Computes the inradius of a regular polygon given the radius.
   * @param radius The radius.
   */
  def inradius(radius: Float, n: Int = sides): Float =
    radius * cos(Pi / n).toFloat

  /**
   * Computes the circumradius of a regular polygon given the inradius.
   * @param inradius The inradius.
   */
  def circumradius(inradius: Float, n: Int = sides): Float =
    inradius / cos(Pi / n).toFloat
}


object RobotColors {
  val ColorBody = ColorRGB(0.05f, 0.05f, 0.05f)
  val ColorHull = ColorRGB(0.95f, 0.95f, 0.95f)
  val ColorThrusters = ColorRGB(0, 0, 1)
  val ColorBackplane = ColorRGB(0.1f, 0.1f, 0.1f)
  val Black = ColorRGB(0, 0, 0)
  val White = ColorRGB(1, 1, 1)
}



case class RobotSignature(
  size: Int,
  engines: Seq[(Engines.type, Int)])

object RobotSignature {
  def apply(robotObject: RobotObject): RobotSignature = {
    val engines =
    for ((Engines, index) <- robotObject.modules.zipWithIndex)
      yield (Engines, index)

    RobotSignature(robotObject.size, engines)
  }
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



// TODO: get better signature
class RobotModelBuilder(robot: RobotObject)(implicit val rs: RenderStack)
  extends ModelBuilder[RobotSignature, RobotObject] {
  def signature: RobotSignature = RobotSignature(robot)

  import Geometry.circumradius
  import RobotColors._
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

    // TODO: ColorBackplane for side #sides - 1
    val hull =
      PolygonOutline(
        rs.MaterialXYRGB,
        sides,
        ColorHull,
        ColorHull,
        radiusBody,
        radiusHull
      ).getModel

    val engines =
      for ((Engines, index) <- signature.engines)
        yield RobotEngines(ModulePosition((sides, index))).getModel

    new RobotModel(body, hull, engines)
  }


}


case class RobotModel(
  body: Model[Unit],
  hull: Model[Unit],
  engines: Seq[Model[Unit]]
) extends CompositeModel[RobotObject] {
  val models = Seq(body, hull) ++ engines

  override def update(a: RobotObject): Unit = {

  }
}



case class RobotEngines(position: VertexXY)(implicit rs: RenderStack)
  extends ModelBuilder[RobotEngines, Unit] {

  import robowars.graphics.models.RobotColors._

  def signature: RobotEngines = this

  protected def buildModel: Model[Unit] = {
    val enginePositions = Geometry.polygonVertices2(3, radius = 5, orientation = 0.4f)
    val engines =
      for ((offset, i) <- enginePositions.zipWithIndex)
      yield new Polygon(
        rs.MaterialXYRGB,
        5,
        ColorThrusters,
        ColorHull,
        radius = 4,
        position = position + offset,
        zPos = 1
      ).getModel

    new StaticCompositeModel(engines)
  }
}
