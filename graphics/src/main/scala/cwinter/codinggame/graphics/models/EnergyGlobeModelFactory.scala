package cwinter.codinggame.graphics.models

import cwinter.codinggame.graphics.engine.RenderStack
import cwinter.codinggame.graphics.model.{Polygon, LinePrimitive}
import cwinter.codinggame.graphics.models.RobotColors.White
import cwinter.codinggame.util.maths.{ColorRGB, ColorRGBA, VertexXY}
import cwinter.codinggame.worldstate.Player


object EnergyGlobeModelFactory {
  def build(position: VertexXY, fade: Float = 1)(implicit rs: RenderStack) = {
    if (fade == 1) {
      Polygon(
        material = rs.BloomShader,
        n = 7,
        colorMidpoint = ColorRGB(1, 1, 1),
        colorOutside = ColorRGB(0, 1, 0),
        radius = 2,
        position = position,
        zPos = 3
      )
    } else {
      Polygon(
        material = rs.TranslucentProportional,
        n = 7,
        colorMidpoint = ColorRGBA(1, 1, 1, fade),
        colorOutside = ColorRGBA(0, 1, 0, fade),
        radius = 2,
        position = position,
        zPos = 3
      )
    }
  }
}