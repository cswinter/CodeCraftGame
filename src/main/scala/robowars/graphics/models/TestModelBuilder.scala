package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._


case class TestModelBuilder(implicit val rs: RenderStack) extends ModelBuilder[TestModelBuilder, Unit] {
  val signature = this
  val sideLength = 50


  protected def buildModel: Model[Unit] = {
    var pos = 0.0f
    def polygonSeries(scale: Int => Float, yPos: Float, color: ColorRGB) = {
      var pos = 1000.0f
      for {
        n <- 3 to 10
        f = scale(n)
      } yield {
        pos += 2.2f * 10 * n
        Polygon(rs.MaterialXYRGB, n, color, color, f, VertexXY(pos, yPos)).getModel
      }
    }

    new StaticCompositeModel(
      polygonSeries(circumradius, 0, ColorRGB(1, 1, 1)) ++
      polygonSeries(_ * 10, 250, ColorRGB(0, 0, 0.5f))
    )
  }


  def circumradius(n: Int) = (sideLength * 0.5 / math.sin(math.Pi / n)).toFloat
}
