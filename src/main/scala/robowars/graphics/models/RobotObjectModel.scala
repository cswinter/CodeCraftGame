package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.matrices.IdentityMatrix4x4
import robowars.graphics.model._
import robowars.graphics.primitives._
import robowars.worldstate.{Engines, StorageModule, WorldObject, RobotObject}

import scala.util.Random


class RobotObjectModel(robot: RobotObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(robot) {

  import math._


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
      new Polygon(20, renderStack.MaterialXYRGB)
        .scale(radius - outlineWidth)
        .color(ColorBackplane)
        .zPos(1)
        .translate(position),
      new PolygonOutline(renderStack.MaterialXYRGB)(20, radius - outlineWidth, radius)
        .color(ColorHull)
        .zPos(1)
        .translate(position)
    )

    val energyPositions = Seq(VertexXY(0, 0)) ++ Geometry.polygonVertices(6, radius = 4.5f)
    val energyGlobes =
      for (i <- 0 until nEnergy)
      yield new Polygon(7, renderStack.BloomShader)
        .scale(2)
        .translate(energyPositions(i) + position)
        .color(ColorRGB(0, 1, 0))
        .colorMidpoint(ColorRGB(1, 1, 1))
        .zPos(2)

    (container ++ energyGlobes).reduce[ComposableModel](_ + _)
  }

  def engineModule(position: VertexXY): ComposableModel = {
    val enginePositions = Geometry.polygonVertices(3, radius = 5, orientation = 0.4f)
    val engines =
      for ((offset, i) <- enginePositions.zipWithIndex)
        yield new Polygon(5, renderStack.MaterialXYRGB)
          .scale(4)
          .colorMidpoint(ColorThrusters)
          .colorOutside(ColorHull)
          .zPos(1)
          .rotate(0.3f * i)
          .translate(position + offset)

    engines.reduce[ComposableModel](_ + _)
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
    new Polygon(sides, renderStack.MaterialXYRGB)
      .scale(radiusBody)
      .color(ColorBody),

    /* hull */
    new PolygonOutline(renderStack.MaterialXYRGB)(sides, radiusBody, radiusHull)
      .color(ColorHull)
      .colorSide(ColorBackplane, sides - 1),

    /* thrusters */
    thruster(1),
    thruster(-1)
  )

  val modules =
    if (ModuleCount.contains(sides)) {
      for {
        (m, i) <- robot.modules.zipWithIndex
        pos = ModulePosition((sides, i))
      }yield m match {
        case StorageModule(r) => storageModule(pos, r)
        case Engines => engineModule(pos)
      }
    } else Seq()


  val thrusterTrails = new MutableWrapperModel(generateThrusterTrails(robot.positions).init())

  val staticModels = (modelComponents ++ modules).reduce[ComposableModel]((x, y) => x + y)
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
  def inradius(radius: Float): Float =
    radius * cos(Pi / sides).toFloat

  /**
   * Computes the circumradius of a regular polygon given the inradius.
   * @param inradius The inradius.
   */
  def circumradius(inradius: Float): Float =
    inradius / cos(Pi / sides).toFloat


}
