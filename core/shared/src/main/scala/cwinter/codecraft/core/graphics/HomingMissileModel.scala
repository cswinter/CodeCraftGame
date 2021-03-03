package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.WorldObjectDescriptor
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.QuadStrip
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}

private[codecraft] case class HomingMissileModel(
  positions: Seq[(Float, Float)],
  nMaxPos: Int,
  playerColor: ColorRGB,
  width: Float
) extends SimpleModelBuilder[HomingMissileModel, Unit]
    with WorldObjectDescriptor[Unit] {
  override protected def model = {
    if (positions.length < 2) EmptyModelBuilder
    else {
      val midpoints = positions.map { case (x, y) => VertexXY(x, y) }
      val n = nMaxPos
      val colorHead = ColorRGB(1, 1, 1)
      val colorTail = playerColor
      val colors = positions.zipWithIndex.map {
        case (_, index) =>
          val x = index / (n - 1).toFloat
          val z = x * x
          ColorRGBA(z * colorHead + (1 - z) * colorTail, x)
      }

      QuadStrip(
        rs.TranslucentAdditive,
        midpoints,
        colors,
        width,
        zPos = 3
      ).noCaching
    }
  }

  override def signature = this
  override def isCacheable = false
  override def allowCaching = false
}
