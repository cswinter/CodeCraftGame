package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model.ColorRGB
import robowars.graphics.primitives.{RichCircleSegment, Polygon, PolygonOutline}
import robowars.worldstate.RobotObject


class RobotObjectModel(robot: RobotObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(robot) {

  val size = robot.size

  val radius = 30
  val hull =
    new PolygonOutline(renderStack.MaterialXYRGB)(5, radius, radius + 6)
      .colorInside(ColorRGB(0.15f, 0.15f, 1f))
      .colorOutside(ColorRGB(0.0f, 0.0f, 1f))

  val body = new Polygon(5, renderStack.MaterialXYRGB)
    .scale(radius)
    .color(ColorRGB(0, 0, 0))

  val sideLength = 2 * (radius + 6) * math.sin(math.Pi / 5).toFloat
  val innerRadius = (radius + 6) * math.cos(math.Pi / 5).toFloat

  val booster1 = new RichCircleSegment(6, 0.7f, renderStack.BloomShader)
    .colorOutside(ColorRGB(0.15f, 0.15f, 1f))
    .colorMidpoint(ColorRGB(0.95f, 0.95f, 0.95f))
    .scaleX(7)
    .scaleY(sideLength / 4.5f)
    .rotate(math.Pi.toFloat)
    .translate(-innerRadius, sideLength / 4)
    .zPos(1)

  val booster2 = new RichCircleSegment(6, 0.7f, renderStack.BloomShader)
    .colorOutside(ColorRGB(0.15f, 0.15f, 1f))
    .colorMidpoint(ColorRGB(0.95f, 0.95f, 0.95f))
    .scaleX(7)
    .scaleY(sideLength / 4.5f)
    .rotate(math.Pi.toFloat)
    .translate(-innerRadius, -sideLength / 4)
    .zPos(1)

  val module1Hull = new PolygonOutline(renderStack.BloomShader)(6, 6, 10)
    .color(ColorRGB(0.0f, 0.0f, 1f))
    //.rotate(2 * math.Pi.toFloat / 10)
    .translate(innerRadius - 3, 0)
    .rotate(2 * math.Pi.toFloat / 10)
    .zPos(-1)

  val weapon1 = new Polygon(6, renderStack.MaterialXYRGB)
    .colorMidpoint(ColorRGB(0.5f, 0.5f, 1))
    .colorOutside(ColorRGB(0, 0, 0))
    .rotate(2 * math.Pi.toFloat / 10)
    .scale(6)
    .translate(innerRadius - 3, 0)
    .rotate(2 * math.Pi.toFloat / 10)
    .zPos(1)


  val module2Hull = new PolygonOutline(renderStack.BloomShader)(6, 6, 10)
    .color(ColorRGB(0.0f, 0.0f, 1f))
    .rotate(2 * math.Pi.toFloat / 10)
    .translate(innerRadius - 3, 0)
    .rotate(-2 * math.Pi.toFloat / 10)
    .zPos(-1)

  val weapon2 = new Polygon(6, renderStack.BloomShader)
    .colorMidpoint(ColorRGB(1, 0.5f, 0))
    .colorOutside(ColorRGB(0.5f, 0, 0))
    //.rotate(2 * math.Pi.toFloat / 10)
    .scale(6)
    .translate(innerRadius - 3, 0)
    .rotate(-2 * math.Pi.toFloat / 10)
    .zPos(2)

  val model = (hull + body + booster1 + booster2 + module1Hull + weapon1 + module2Hull + weapon2).init()
}
