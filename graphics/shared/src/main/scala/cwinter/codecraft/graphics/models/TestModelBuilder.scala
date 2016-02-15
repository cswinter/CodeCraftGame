package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}


private[graphics] case class TestModelBuilder(t: Int)(implicit rs: RenderStack) extends ModelBuilder[TestModelBuilder, Unit] {
  val signature = this
  val sideLength = 50


  protected def buildModel: Model[Unit] = {
    EmptyModel
  }


  def circumradius(n: Int) = (sideLength * 0.5 / math.sin(math.Pi / n)).toFloat

  override def isCacheable: Boolean = false
}


private[graphics] case class ProcessingModuleModelBuilder(positions: Seq[VertexXY], t: Int, tMerging: Option[Int], size: Int)(implicit rs: RenderStack) extends ModelBuilder[ProcessingModuleModelBuilder, Unit] {
  def signature: ProcessingModuleModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    // triangle 1: 0, 2, 4
    // triangle 2: 1, 3, 5
    val x = t / 50f
    val transitions =
      if (x <= 1) Seq(0 -> 3, 2 -> 1, 4 -> 5)
      else Seq(1 -> 4, 3 -> 2, 5 -> 0)
    val progress = if (x <= 1) x else x - 1

    val center = positions.reduce(_ + _) / positions.size
    val scale = math.sqrt(size).toFloat

    tMerging match {
      case None =>
        new StaticCompositeModel(
          computeModelComponents(
            center, progress, x * math.Pi.toFloat, 1, scale, transitions))
      case Some(n) =>
        val xM = n / 100f
        val fade = (1 - xM) * 1 + xM / size
        val components =
          for {
            origin <- positions
            pos = xM * center + (1 - xM) * origin
            component <- computeModelComponents(pos, progress, x * math.Pi.toFloat, fade, 1 + (scale - 1) * xM, transitions)
          } yield component
        new StaticCompositeModel(components)
    }
  }


  def computeModelComponents(position: VertexXY, progress: Float, rotation: Float, fade: Float, scale: Float, transitions: Seq[(Int, Int)]): Seq[Model[Unit]] = {
    val radius = 5f * scale
    val radius2 = 3f * scale
    val positions: Seq[VertexXY] =
      for (n <- 0 until 6)
        yield radius * VertexXY(math.Pi.toFloat * n / 3 + rotation)

    val poss =
      for ((i1, i2) <- transitions)
        yield progress * positions(i1) + (1 - progress) * positions(i2)

    val BaseCol = ColorRGB(0.3f, 0.3f, 0.3f)
    for (pos <- poss ++ Seq(radius2 * VertexXY(-rotation + 1), radius2 * VertexXY(math.Pi.toFloat - rotation + 1)))
      yield
      Polygon(
        rs.GaussianGlow,
        50,
        ColorRGBA(BaseCol * fade, 1),
        ColorRGBA(BaseCol * fade, 0),
        10 * scale,
        position + pos
      ).getModel
  }
}
