package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.WorldObjectDescriptor
import cwinter.codecraft.graphics.model.{SimpleModelBuilder, Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}

private[codecraft] case class BasicHomingMissileModel(
  x: Float,
  y: Float,
  playerColor: ColorRGB,
  width: Float
) extends SimpleModelBuilder[BasicHomingMissileModel, Unit]
    with WorldObjectDescriptor[Unit] {

  def model =
    Polygon(
      material = rs.TranslucentAdditive,
      n = 10,
      colorMidpoint = ColorRGBA(1, 1, 1, 1),
      colorOutside = ColorRGBA(playerColor, 1),
      radius = width,
      position = VertexXY(x, y),
      zPos = 3
    ).noCaching

  override def signature = this
  override def isCacheable = false
  override def allowCaching = false
}
