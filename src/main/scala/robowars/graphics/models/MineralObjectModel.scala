package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model.{Polygon, DrawableModelBridge, ColorRGB}
import robowars.worldstate.MineralObject


class MineralObjectModel(mineral: MineralObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(mineral) {

  val size = mineral.size
  val radius = math.sqrt(size).toFloat * 15

  val model =
    new DrawableModelBridge(
      Polygon(
        renderStack.BloomShader,
        n = 5,
        colorMidpoint = ColorRGB(0.03f, 0.6f, 0.03f),
        colorOutside = ColorRGB(0.0f, 0.1f, 0.0f),
        radius = radius,
        zPos = -1
      ).getModel
    )
}
