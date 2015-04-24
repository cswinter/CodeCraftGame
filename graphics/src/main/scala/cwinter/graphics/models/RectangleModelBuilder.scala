package cwinter.graphics.models

import cwinter.codinggame.util.maths
import cwinter.codinggame.util.maths.ColorRGB
import cwinter.graphics.engine.RenderStack
import cwinter.graphics.model._


case class RectangleModelBuilder(rectangle: maths.Rectangle)(implicit val rs: RenderStack)
  extends ModelBuilder[RectangleModelBuilder, Unit] {
  val signature = this

  override protected def buildModel: Model[Unit] = {
    RectanglePrimitive(
      rs.MaterialXYRGB,
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
