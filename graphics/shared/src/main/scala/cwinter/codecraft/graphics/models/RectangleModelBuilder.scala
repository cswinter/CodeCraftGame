package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{Model, ModelBuilder, RectanglePrimitive}
import cwinter.codecraft.util.maths
import cwinter.codecraft.util.maths.ColorRGB


case class RectangleModelBuilder(rectangle: maths.Rectangle)(implicit val rs: RenderStack)
  extends ModelBuilder[RectangleModelBuilder, Unit] {
  val signature = this

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
}
