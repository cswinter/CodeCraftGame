package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._


case class TestModelBuilder(t: Int)(implicit rs: RenderStack) extends ModelBuilder[TestModelBuilder, Unit] {
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
      polygonSeries(_ * 10, 250, ColorRGB(0, 0, 0.5f)) :+
      new FactoryModelBuilder(NullVectorXY, t % 250).getModel
    )
  }


  def circumradius(n: Int) = (sideLength * 0.5 / math.sin(math.Pi / n)).toFloat
}



case class FactoryModelBuilder(position: VertexXY, t: Int)(implicit rs: RenderStack) extends ModelBuilder[FactoryModelBuilder, Unit] {
  def signature: FactoryModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    val Frames = 250
    val Speed = 5
    val Interval = Frames / Speed
    val x = t / 5
    val cycle = if (x < Interval / 2) 2 * x else 2 * (Interval - x)

    val insideColor = ColorRGBA(1, 1, 1, 0.4f + cycle * 0.4f / Interval)
    val outsideColor = ColorRGBA(1, 1, 1, 0.8f - cycle * 0.4f / Interval)
    val radius = 8

    Polygon(
      rs.GaussianGlow,
      50,
      insideColor,
      outsideColor,
      radius,
      position,
      2
    ).getModel
  }
}