package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.materials.Intensity
import robowars.graphics.matrices.DilationXYMatrix4x4
import robowars.graphics.model._
import robowars.graphics.primitives.PolygonOld
import robowars.worldstate.{WorldObject, LightFlash}


class LightFlashObjectModel(lightFlash: LightFlash)(implicit val rs: RenderStack)
  extends WorldObjectModel(lightFlash) {
/*
  val lightFlashModel =
    new PolygonOld(25, renderStack.GaussianGlowPIntensity)
      .colorMidpoint(ColorRGBA(1, 1, 1, 0))
      .colorOutside(ColorRGBA(1, 1, 1, 1))
      .scale(1)
      .zPos(-1)
      .initParameterized(renderStack.GaussianGlowPIntensity)

  val model = lightFlashModel

  override def update(worldObject: WorldObject): this.type = {
    super.update(worldObject)

    val lightFlash = worldObject.asInstanceOf[LightFlash]

    val modelview = new DilationXYMatrix4x4(60 * lightFlash.stage + 5) * model.modelview
    model.setModelview(modelview)

    lightFlashModel.params = Intensity(1 - lightFlash.stage)

    this
  }*/

  val actualModel = new LightFlashModelBuilder(lightFlash).getModel
  val model = new DrawableModelBridge(actualModel)

  override def update(worldObject: WorldObject): this.type = {
    super.update(worldObject)
    val lightFlash = worldObject.asInstanceOf[LightFlash]
    actualModel.update(lightFlash)
    this
  }

}

case class LightFlashSign(rs: RenderStack)

class LightFlashModelBuilder(lightFlash: LightFlash)(implicit val rs: RenderStack)
  extends ModelBuilder[LightFlashSign, LightFlash] {
  val signature = LightFlashSign(rs)

  override protected def buildModel: Model[LightFlash] = {
    val flash = new Polygon(
      rs.GaussianGlowPIntensity,
      25,
      ColorRGBA(1, 1, 1, 0),
      ColorRGBA(1, 1, 1, 1),
      radius = 1,
      zPos = -1
    ).getModel

    new LightFlashModel(flash)
  }
}


class LightFlashModel(val flash: Model[Intensity]) extends CompositeModel[LightFlash] {
  val models = Seq(flash)

  override def update(lightFlash: LightFlash): Unit = {
    flash.update(Intensity(1 - lightFlash.stage))
  }
}

/*
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
 */