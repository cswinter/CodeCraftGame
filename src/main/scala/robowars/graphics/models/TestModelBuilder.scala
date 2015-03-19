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

  override def isCacheable: Boolean = false
}


case class FactoryModelBuilder(position: VertexXY, t: Int)(implicit rs: RenderStack) extends ModelBuilder[FactoryModelBuilder, Unit] {
  def signature: FactoryModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    // triangle 1: 0, 2, 4
    // triangle 2: 1, 3, 5
    val x = t / 125f
    if (x <= 1) calculateModel(x, x * math.Pi.toFloat, Seq(0 -> 3, 2 -> 1, 4 -> 5))
    else calculateModel(x - 1, x * math.Pi.toFloat, Seq(1 -> 4, 3 -> 2, 5 -> 0))
  }

  def calculateModel(progress: Float, rotation: Float, transitions: Seq[(Int, Int)]): Model[Unit] = {
    val radius = 5f
    val radius2 = 3f
    val positions: Seq[VertexXY] =
      for (n <- 0 until 6)
      yield radius * VertexXY(math.Pi.toFloat * n / 3 + rotation)

    val poss =
      for ((i1, i2) <- transitions)
        yield progress * positions(i1) + (1 - progress) * positions(i2)

    val BaseCol = ColorRGB(0.3f, 0.3f, 0.3f)
    new StaticCompositeModel(
      for (pos <- poss ++ Seq(radius2 * VertexXY(-rotation + 1), radius2 * VertexXY(math.Pi.toFloat - rotation + 1))) yield
        Polygon(
          rs.GaussianGlow,
          50,
          ColorRGBA(BaseCol, 0),
          ColorRGBA(BaseCol, 1),
          10,
          position + pos
        ).getModel
    )
  }
}
