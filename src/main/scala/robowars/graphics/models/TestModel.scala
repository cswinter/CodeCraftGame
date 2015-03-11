package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model.{EmptyModel, Model, ColorRGB}
import robowars.graphics.primitives.Polygon
import robowars.worldstate.MineralObject


class TestModel(implicit val rs: RenderStack) extends WorldObjectModel(MineralObject(-1, 0, 0, 0, 1)) {
  val sideLength = 50


  def polygonSeries(scale: Int => Float, yPos: Float, color: ColorRGB) = {
    var pos = 1000.0f
    for (n <- 3 to 10)
    yield {
      val f = scale(n)
      pos += 2.2f * 10 * n
      regularPolygon(n, f).translate(pos, yPos).color(color)
    }
  }

  var pos = 0.0f
  val models = polygonSeries(circumradius, 0, ColorRGB(1, 1, 1))
  val models2 = polygonSeries(_ * 10, 250, ColorRGB(0, 0, 0.5f))


  val m1 = models.foldLeft[Model](EmptyModel)((x, y) => x + y)
  val m2 = models2.foldLeft[Model](m1)((x, y) => x + y)
  val model = m2.init()

  def regularPolygon(n: Int, f: Float): Polygon[ColorRGB] = {
    new Polygon(n, renderStack.MaterialXYRGB)
      .color(ColorRGB(0, 1, 0))
      .scale(f)
  }

  def circumradius(n: Int) = (sideLength * 0.5 / math.sin(math.Pi / n)).toFloat
}
