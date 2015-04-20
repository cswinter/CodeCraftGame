package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import robowars.worldstate.MineralDescriptor


case class MineralSignature(size: Int)

class MineralModelBuilder(mineral: MineralDescriptor)(implicit val rs: RenderStack)
  extends ModelBuilder[MineralSignature, Unit] {
  val signature = MineralSignature(mineral.size)

  override protected def buildModel: Model[Unit] = {
    val size = mineral.size
    val radius = math.sqrt(size).toFloat * 15

    Polygon(
      rs.BloomShader,
      n = 5,
      colorMidpoint = ColorRGB(0.03f, 0.6f, 0.03f),
      colorOutside = ColorRGB(0.0f, 0.1f, 0.0f),
      radius = radius,
      zPos = -1
    ).getModel
  }
}
