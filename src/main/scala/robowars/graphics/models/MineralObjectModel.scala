package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model.ColorRGB
import robowars.graphics.primitives.Polygon
import robowars.worldstate.MineralObject


class MineralObjectModel(mineral: MineralObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(mineral) {

  val size = mineral.size
  val radius = math.sqrt(size).toFloat * 15

  val model =
    new Polygon(5, renderStack.BloomShader)
      .colorMidpoint(ColorRGB(0.03f, 0.6f, 0.03f))
      .colorOutside(ColorRGB(0.0f, 0.1f, 0.0f))
      .scale(radius)
      .zPos(-1)
      .init()
}
