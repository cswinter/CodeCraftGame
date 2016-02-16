package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.materials.Intensity
import cwinter.codecraft.graphics.model.{ProjectedParamsModel, CompositeModel, Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.graphics.worldstate.LightFlashDescriptor
import cwinter.codecraft.util.maths.ColorRGBA


private[graphics] case class LightFlashSignature(rs: RenderStack) extends AnyRef

private[graphics] class LightFlashModelBuilder(implicit val rs: RenderStack)
  extends ModelBuilder[LightFlashSignature, LightFlashDescriptor] {
  val signature = LightFlashSignature(rs)

  override protected def buildModel: Model[LightFlashDescriptor] = {
    val flash = Polygon(
      rs.GaussianGlowPIntensity,
      25,
      ColorRGBA(1, 1, 1, 1),
      ColorRGBA(1, 1, 1, 0),
      radius = 1,
      zPos = -1
    ).getModel.scalable(rs.modelviewTranspose)

    val flashConnected =
      flash.wireParameters[LightFlashDescriptor]{
        lf =>
          val intensity = Intensity(1 - lf.stage)
          val radius = 60 * lf.stage + 5
          (intensity, radius)
      }

    CompositeModel(
      Seq.empty,
      Seq(flashConnected)
    )
  }
}

