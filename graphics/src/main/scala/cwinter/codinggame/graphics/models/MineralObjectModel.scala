package cwinter.codinggame.graphics.models

import cwinter.codinggame.graphics.engine.RenderStack
import cwinter.codinggame.graphics.model.{Model, ModelBuilder, Polygon}
import cwinter.codinggame.util.maths.ColorRGB
import cwinter.codinggame.worldstate.MineralDescriptor


case class MineralSignature(size: Int, harvested: Boolean)

class MineralModelBuilder(mineral: MineralDescriptor)(implicit val rs: RenderStack)
  extends ModelBuilder[MineralSignature, Unit] {
  val signature = MineralSignature(mineral.size, mineral.harvested)

  override protected def buildModel: Model[Unit] = {
    val size = mineral.size
    val radius = math.sqrt(size).toFloat * 8

    Polygon(
      rs.BloomShader,
      n = 5,
      colorMidpoint = ColorRGB(0.03f, 0.6f, 0.03f),
      colorOutside = ColorRGB(0.0f, 0.1f, 0.0f),
      radius = radius,
      zPos = if (signature.harvested) 2 else -1
    ).getModel
  }
}
