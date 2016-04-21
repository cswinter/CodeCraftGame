package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.WorldObjectDescriptor
import cwinter.codecraft.graphics.model.{Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.RectanglePrimitive
import cwinter.codecraft.util.maths
import cwinter.codecraft.util.maths.ColorRGB


private[codecraft] case class RectangleModelBuilder(rectangle: maths.Rectangle)
  extends ModelBuilder[RectangleModelBuilder, Unit] with WorldObjectDescriptor[Unit] {

  override protected def buildModel: Model[Unit] = {
    RectanglePrimitive(
      rs.MaterialXYZRGB,
      rectangle.xMin.toFloat,
      rectangle.xMax.toFloat,
      rectangle.yMin.toFloat,
      rectangle.yMax.toFloat,
      3,
      ColorRGB(0.7f, 0.7f, 0.7f),
      0
    ).getModel
  }

  override protected def createModel(timestep: Int) = getModel
  override def signature = this
}
