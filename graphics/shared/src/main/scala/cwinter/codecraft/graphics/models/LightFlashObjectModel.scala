package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.materials.Intensity
import cwinter.codecraft.graphics.model.{ProjectedParamsModel, CompositeModel, Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.graphics.worldstate.LightFlashDescriptor
import cwinter.codecraft.util.maths.ColorRGBA


private[graphics] case class LightFlashSign(rs: RenderStack)

private[graphics] class LightFlashModelBuilder(lightFlash: LightFlashDescriptor)(implicit val rs: RenderStack)
  extends ModelBuilder[LightFlashSign, LightFlashDescriptor] {
  val signature = LightFlashSign(rs)

  override protected def buildModel: Model[LightFlashDescriptor] = {
    val flash = Polygon(
      rs.GaussianGlowPIntensity,
      25,
      ColorRGBA(1, 1, 1, 1),
      ColorRGBA(1, 1, 1, 0),
      radius = 1,
      zPos = -1
    ).getModel.scalable(rs.modelviewTranspose)

    val flashConnected = ProjectedParamsModel(
      flash,
      (lf: LightFlashDescriptor) => {
        val intensity = Intensity(1 - lightFlash.stage)
        val radius = 60 * lightFlash.stage + 5
        (intensity, radius)
      }
    )

    CompositeModel(
      Seq.empty,
      Seq(flashConnected)
    )
  }
}

