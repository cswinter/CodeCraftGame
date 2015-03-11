package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.materials.Intensity
import robowars.graphics.matrices.DilationXYMatrix4x4
import robowars.graphics.model.ColorRGBA
import robowars.graphics.primitives.Polygon
import robowars.worldstate.{WorldObject, LightFlash}


class LightFlashObjectModel(lightFlash: LightFlash)(implicit val rs: RenderStack)
  extends WorldObjectModel(lightFlash) {

  val lightFlashModel =
    new Polygon(25, renderStack.GaussianGlowPIntensity)
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
  }
}
