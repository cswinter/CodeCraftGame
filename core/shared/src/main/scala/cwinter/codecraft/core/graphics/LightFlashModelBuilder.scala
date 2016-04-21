package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.WorldObjectDescriptor
import cwinter.codecraft.graphics.materials.Intensity
import cwinter.codecraft.graphics.model.{CompositeModel, Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.util.maths.ColorRGBA


private[codecraft] case object LightFlashModelBuilder
  extends ModelBuilder[Any, Float] with WorldObjectDescriptor[Float] {

  override protected def buildModel: Model[Float] = {
    val flash = Polygon(
      rs.GaussianGlowPIntensity,
      25,
      ColorRGBA(1, 1, 1, 1),
      ColorRGBA(1, 1, 1, 0),
      radius = 1,
      zPos = -1
    ).getModel.scalable(rs.modelviewTranspose)

    val flashConnected =
      flash.wireParameters[Float]{
        stage =>
          val intensity = Intensity(1 - stage)
          val radius = 60 * stage + 5
          (intensity, radius)
      }

    CompositeModel(
      Seq.empty,
      Seq(flashConnected)
    )
  }

  override protected def createModel(timestep: Int) = getModel
  override def signature = this
}

