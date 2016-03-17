package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.EmptyModel
import cwinter.codecraft.graphics.primitives.{Polygon, QuadStrip}
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}


private[graphics] object BasicHomingMissileModelFactory {
  def build(xPos: Float, yPos: Float, playerColor: ColorRGB)(implicit rs: RenderStack) = {
    Polygon(
      material = rs.TranslucentAdditive,
      n = 10,
      colorMidpoint = ColorRGBA(1, 1, 1, 1),
      colorOutside = ColorRGBA(playerColor, 1),
      radius = 5,
      position = VertexXY(xPos, yPos),
      zPos = 3
    ).noCaching.getModel
  }
}