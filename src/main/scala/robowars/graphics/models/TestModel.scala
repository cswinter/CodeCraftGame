package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import robowars.graphics.primitives.{Primitive2D, NewPolygonOutline$, PolygonOld$}
import robowars.worldstate.MineralObject


class TestModel(implicit val rs: RenderStack) extends WorldObjectModel(MineralObject(-1, 0, 0, 0, 1)) {
  val sideLength = 50


  def polygonSeries(scale: Int => Float, yPos: Float, color: ColorRGB) = {
    var pos = 1000.0f
    for {
      n <- 3 to 10
      f = scale(n)
    } yield {
      pos += 2.2f * 10 * n
      new DrawableModelBridge(
        Polygon(renderStack.MaterialXYRGB, n, color, color, f, VertexXY(pos, yPos)).getModel)
    }
  }

  var pos = 0.0f
  val models = polygonSeries(circumradius, 0, ColorRGB(1, 1, 1))
  val models2 = polygonSeries(_ * 10, 250, ColorRGB(0, 0, 0.5f))

  val m1 = models.reduce[DrawableModel](_ * _)
  val m2 = models2.foldLeft(m1)(_ * _)
  val model = m2


  def circumradius(n: Int) = (sideLength * 0.5 / math.sin(math.Pi / n)).toFloat
}
