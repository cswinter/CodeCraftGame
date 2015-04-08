package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.materials.Intensity
import robowars.graphics.model._
import robowars.worldstate.LightFlash


case class LightFlashSign(rs: RenderStack)

class LightFlashModelBuilder(lightFlash: LightFlash)(implicit val rs: RenderStack)
  extends ModelBuilder[LightFlashSign, LightFlash] {
  val signature = LightFlashSign(rs)

  override protected def buildModel: Model[LightFlash] = {
    val flash = Polygon(
      rs.GaussianGlowPIntensity,
      25,
      ColorRGBA(1, 1, 1, 0),
      ColorRGBA(1, 1, 1, 1),
      radius = 1,
      zPos = -1
    ).getModel.scalable

    new LightFlashModel(flash)
  }
}


class LightFlashModel(val flash: Model[(Intensity, Float)]) extends CompositeModel[LightFlash] {
  val models = Seq(flash)

  override def update(lightFlash: LightFlash): Unit = {
    val intensity = Intensity(1 - lightFlash.stage)
    val radius = 60 * lightFlash.stage + 5

    flash.update((intensity, radius))
  }
}
