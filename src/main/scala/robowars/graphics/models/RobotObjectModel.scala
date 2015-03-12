package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.matrices.IdentityMatrix4x4
import robowars.graphics.model._
import robowars.graphics.primitives._
import robowars.worldstate.{WorldObject, RobotObject}

import scala.util.Random


class RobotObjectModel(robot: RobotObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(robot) {

  import math._

  val sides = robot.size
  val sideLength = 40
  val radiusBody = 0.5f * sideLength / sin(Pi / sides).toFloat
  val radiusHull = radiusBody + circumradius(4)


  val ColorBody = ColorRGB(0.05f, 0.05f, 0.05f)
  val ColorHull = ColorRGB(0.95f, 0.95f, 0.95f)
  val ColorThrusters = if (robot.identifier % 2 == 0) ColorRGB(0, 0, 1) else ColorRGB(1, 0, 0)
  val ColorBackplane = ColorRGB(0.1f, 0.1f, 0.1f)
  val Black = ColorRGB(0, 0, 0)


  def storageModule(): ComposableModel = {
    new Polygon(20, renderStack.MaterialXYRGB)
      .scale(8)
      .color(ColorBackplane)
      .zPos(1) +
    new PolygonOutline(renderStack.MaterialXYRGB)(20, 8, 9)
      .color(ColorHull)
      .zPos(1)
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
    thruster(-1),

    /* storage module */
    storageModule()
  )

  val thrusterTrails = new MutableWrapperModel(generateThrusterTrails(robot.positions).init())

  val model = modelComponents.reduce[ComposableModel]((x, y) => x + y).init() * thrusterTrails


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
