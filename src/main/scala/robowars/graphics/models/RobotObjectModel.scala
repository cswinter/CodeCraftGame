package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import robowars.graphics.primitives._
import robowars.worldstate.RobotObject


class RobotObjectModel(robot: RobotObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(robot) {

  import math._

  val sides = robot.size
  val sideLength = 40
  val radiusBody = 0.5f * sideLength / sin(Pi / sides).toFloat
  val radiusHull = radiusBody + circumradius(4)


  val HullColor = ColorRGB(0.95f, 0.95f, 0.95f)
  val ThrusterColor = ColorRGB(0, 0, 1)


  val modelComponents = Seq(
    /* body */
    new Polygon(sides, renderStack.MaterialXYRGB)
      .scale(radiusBody)
      .color(ColorRGB(0.05f, 0.05f, 0.05f)),

    /* hull */
    new PolygonOutline(renderStack.MaterialXYRGB)(sides, radiusBody, radiusHull)
      .color(HullColor),

    /* thrusters */
    thruster(1),
    thruster(-1)
  )


  def thruster(side: Int) = {
    val perp = outerModulePerpendicular(0)
    new RichCircleSegment(8, 0.7f, renderStack.MaterialXYRGB)
      .scaleX(5)
      .scaleY(sideLength * 0.25f)
      .rotate(Pi.toFloat)
      .translate(outerModulePosition(0))
      .translate(side * sideLength * 0.3f * perp)
      .colorMidpoint(ThrusterColor)
      .colorOutside(HullColor)
      .zPos(1)
  }

  val model = modelComponents.reduce((x: Model, y: Model) => x + y).init()


  def outerModulePosition(n: Int): VertexXY = {
    assert(sides > n)
    assert(n >= 0)
    val r = inradius(radiusHull)
    r * outerModuleNormal(n)
  }


  def outerModuleNormal(n: Int): VertexXY = {
    val angle = Pi + (2 * n * Pi / sides)
    VertexXY(angle)
  }

  def outerModulePerpendicular(n: Int): VertexXY = {
    outerModuleNormal(n).perpendicular
  }


  /**
   * Computes the inradius of a regular polygon given the radius.
   * @param radius The radius.
   */
  def inradius(radius: Float): Float =
    radius * cos(Pi / sides).toFloat

  /**
   * Computes the circumradius of a regular polygon given the inradius.
   * @param n The number of sides.
   * @param inradius The inradius.
   */
  def circumradius(inradius: Float): Float =
    inradius / cos(Pi / sides).toFloat


}
