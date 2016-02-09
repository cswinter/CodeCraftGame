package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.LinePrimitive
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}


private[graphics] object ManipulatorArmModelFactory {
  def build(playerColor: ColorRGB, x1: Float, y1: Float, x2: Float, y2: Float)(implicit rs: RenderStack) = {

    LinePrimitive(
      rs.GaussianGlow,
      VertexXY(x1, y1),
      VertexXY(x2, y2),
      3,
      ColorRGBA(0.5f * playerColor + 0.5f * DefaultDroneColors.White, 1),
      ColorRGBA(DefaultDroneColors.White, 0),
      zPos = 3
    ).noCaching.getModel
  }
}