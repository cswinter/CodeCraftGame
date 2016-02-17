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


