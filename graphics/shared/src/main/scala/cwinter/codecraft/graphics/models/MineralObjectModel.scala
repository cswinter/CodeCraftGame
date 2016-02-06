package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{Model, ModelBuilder, Polygon}
import cwinter.codecraft.graphics.worldstate.MineralDescriptor
import cwinter.codecraft.util.PrecomputeHash
import cwinter.codecraft.util.maths.{ColorRGBA, ColorRGB}


private[graphics] case class MineralSignature(size: Int, harvested: Boolean, harvestingProgress: Option[Float])
extends PrecomputeHash

private[graphics] class MineralModelBuilder(mineral: MineralDescriptor)(implicit val rs: RenderStack)
  extends ModelBuilder[MineralSignature, Unit] {
  val signature = mineral.signature

  override protected def buildModel: Model[Unit] = {
    val size = mineral.size
    val radius = math.sqrt(size).toFloat * 6

    mineral.harvestingProgress match {
      case Some(p) =>
        Polygon(
          rs.TranslucentProportional,
          n = 5,
          colorMidpoint = ColorRGBA(0.03f, 0.6f, 0.03f, p),
          colorOutside = ColorRGBA(0.0f, 0.1f, 0.0f, p),
          radius = radius,
          zPos = 2
        ).getModel
      case None =>
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
}
