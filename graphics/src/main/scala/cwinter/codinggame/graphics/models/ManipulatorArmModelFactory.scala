package cwinter.codinggame.graphics.models

import cwinter.codinggame.graphics.engine.RenderStack
import cwinter.codinggame.graphics.model.LinePrimitive
import cwinter.codinggame.util.maths.{ColorRGB, ColorRGBA, VertexXY}
import RobotColors.White
import cwinter.codinggame.worldstate.Player


object ManipulatorArmModelFactory {
  def build(player: Player, x1: Float, y1: Float, x2: Float, y2: Float)(implicit rs: RenderStack) = {

    LinePrimitive(
      rs.GaussianGlow,
      VertexXY(x1, y1),
      VertexXY(x2, y2),
      3,
      ColorRGBA(0.5f * player.color + 0.5f * White, 0),
      ColorRGBA(White, 1),
      zPos = 3
    ).noCaching.getModel
  }
}