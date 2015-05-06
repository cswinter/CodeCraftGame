package cwinter.graphics.models

import cwinter.codinggame.util.maths.{ColorRGB, ColorRGBA, VertexXY}
import cwinter.graphics.engine.RenderStack
import cwinter.graphics.model._
import cwinter.worldstate.Player
import cwinter.graphics.models.RobotColors.White


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