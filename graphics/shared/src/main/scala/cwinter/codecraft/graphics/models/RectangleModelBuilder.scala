package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.{GraphicsContext, WorldObjectDescriptor}
import cwinter.codecraft.graphics.model.{Model, SimpleModelBuilder}
import cwinter.codecraft.graphics.primitives.RectanglePrimitive
import cwinter.codecraft.util.maths
import cwinter.codecraft.util.maths.ColorRGB


private[codecraft] case class RectangleModelBuilder(rectangle: maths.Rectangle)
  extends SimpleModelBuilder[RectangleModelBuilder, Unit] with WorldObjectDescriptor[Unit] {

  override protected def model =
    RectanglePrimitive(
      rs.MaterialXYZRGB,
      rectangle.xMin.toFloat,
      rectangle.xMax.toFloat,
      rectangle.yMin.toFloat,
      rectangle.yMax.toFloat,
      3,
      ColorRGB(0.7f, 0.7f, 0.7f),
      0
    )

  override def signature = this
}
