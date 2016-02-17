package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.graphics.worldstate.MineralDescriptor
import cwinter.codecraft.util.PrecomputedHashcode
import cwinter.codecraft.util.maths.ColorRGB


private[graphics] case class MineralSignature(size: Int) extends PrecomputedHashcode

private[graphics] class MineralModelBuilder(mineral: MineralDescriptor)(implicit val rs: RenderStack)
  extends ModelBuilder[MineralSignature, Unit] {
  val signature = mineral.signature

  override protected def buildModel: Model[Unit] = {
    val size = mineral.size
    val radius = math.sqrt(size).toFloat * 6

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
