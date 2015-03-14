package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.matrices.IdentityMatrix4x4
import robowars.graphics.model._
import robowars.graphics.primitives._
import robowars.worldstate._
import scala.math._
import robowars.graphics.models.RobotColors._
import Geometry._


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


  val thrusterTrails = new MutableWrapperModel(generateThrusterTrails(robot.positions).init())

  val staticModels = (modelComponents :+ shield).reduce[ComposableModel]((x, y) => x + y)
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
  engines: Seq[(Engines.type, Int)],
  storageModules: Seq[(StorageModule, Int)],
  shieldGeneratorModels: Seq[(ShieldGenerator.type, Int)]
)

object RobotSignature {
  def apply(robotObject: RobotObject): RobotSignature = {
    val engines =
      for ((Engines, index) <- robotObject.modules.zipWithIndex)
      yield (Engines, index)

    val storageModules =
      for ((storage: StorageModule, index) <- robotObject.modules.zipWithIndex)
      yield (storage, index)

    val shieldGeneratorModels =
      for ((ShieldGenerator, index) <- robotObject.modules.zipWithIndex)
      yield (ShieldGenerator, index)

    RobotSignature(robotObject.size, engines, storageModules, shieldGeneratorModels)
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

    val storageModules =
      for ((StorageModule(count), index) <- signature.storageModules)
      yield RobotStorageModule(ModulePosition((sides, index)), count).getModel

    val shieldGeneratorModules =
      for ((ShieldGenerator, index) <- signature.shieldGeneratorModels)
      yield ShieldGeneratorModel(ModulePosition((sides, index))).getModel

    new RobotModel(body, hull, engines, storageModules, shieldGeneratorModules)
  }


}


case class RobotModel(
  body: Model[Unit],
  hull: Model[Unit],
  engines: Seq[Model[Unit]],
  storageModules: Seq[Model[Unit]],
  shieldGeneratorModules: Seq[Model[Unit]]
) extends CompositeModel[RobotObject] {
  // MAKE SURE TO ADD NEW COMPONENTS HERE:
  val models = Seq(body, hull) ++ engines ++ storageModules ++ shieldGeneratorModules

  override def update(a: RobotObject): Unit = {

  }
}


case class RobotEngines(position: VertexXY)(implicit rs: RenderStack)
  extends ModelBuilder[RobotEngines, Unit] {

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

case class RobotStorageModule(position: VertexXY, nEnergyGlobes: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotStorageModule, Unit] {


  def signature = this

  protected def buildModel: Model[Unit] = {
    val radius = 8
    val outlineWidth = 1
    val body =
      Polygon(
        material = rs.MaterialXYRGB,
        n = 20,
        colorMidpoint = ColorBackplane,
        colorOutside = ColorBackplane,
        radius = radius - outlineWidth,
        position = position,
        zPos = 1
      ).getModel

    val hull =
      PolygonOutline(
        material = rs.MaterialXYRGB,
        n = 20,
        colorInside = ColorHull,
        colorOutside = ColorHull,
        innerRadius = radius - outlineWidth,
        outerRadius = radius,
        position = position,
        zPos = 1
      ).getModel

    val energyPositions = Seq(VertexXY(0, 0)) ++ Geometry.polygonVertices2(6, radius = 4.5f)
    val energyGlobes =
      for (i <- 0 until nEnergyGlobes)
      yield
        Polygon(
          material = rs.BloomShader,
          n = 7,
          colorMidpoint = ColorRGB(1, 1, 1),
          colorOutside = ColorRGB(0, 1, 0),
          radius = 2,
          position = energyPositions(i) + position,
          zPos = 2
        ).getModel

    new StaticCompositeModel(body +: hull +: energyGlobes)
  }
}

case class ShieldGeneratorModel(position: VertexXY)(implicit rs: RenderStack)
  extends ModelBuilder[ShieldGeneratorModel, Unit] {
  def signature = this


  protected def buildModel: Model[Unit] = {
    val radius = 3
    val gridpointRadius = 2 * inradius(radius, 6)
    val gridpoints = VertexXY(0, 0) +: Geometry.polygonVertices(6, radius = gridpointRadius)
    val hexgrid =
      for (pos <- gridpoints)
      yield
        new PolygonOutline(
          material = rs.MaterialXYRGB,
          n = 6,
          colorInside = White,
          colorOutside = White,
          innerRadius = radius - 0.5f,
          outerRadius = radius,
          position = pos + position,
          zPos = 1
        ).getModel

    val filling =
      for (pos <- gridpoints)
      yield
        new Polygon(
          material = rs.MaterialXYRGB,
          n = 6,
          colorMidpoint = ColorThrusters,
          colorOutside = ColorThrusters,
          radius = radius - 0.5f,
          position = pos + position,
          zPos = 1
        ).getModel

    new StaticCompositeModel(hexgrid ++ filling)
  }
}