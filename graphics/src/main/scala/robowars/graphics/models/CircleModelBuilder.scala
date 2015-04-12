package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import robowars.worldstate.MineralObject


case class CircleModelBuilder(radius: Float, id: Int)(implicit val rs: RenderStack)
  extends ModelBuilder[CircleModelBuilder, Unit] {
  val signature = this
  val ColorCode = false

  override protected def buildModel: Model[Unit] = {
    Polygon(
      rs.MaterialXYRGB,
      n = 50,
      colorMidpoint = if (ColorCode) Colors(id / 10) else ColorRGB(0.0f, 0.0f, 0.4f),
      colorOutside = if (ColorCode) Colors(id % 10) else ColorRGB(0.7f, 0.7f, 0.7f),
      radius = radius,
      zPos = 0
    ).getModel
  }


  val Colors = IndexedSeq(
    ColorRGB(0, 0, 0),
    ColorRGB(0.5f, 0.5f, 0.5f),
    ColorRGB(1, 1, 1),

    ColorRGB(1, 0, 0),
    ColorRGB(0, 1, 0),
    ColorRGB(0, 0, 1),

    ColorRGB(0.75f, 0.75f, 0),
    ColorRGB(0.75f, 0, 0.75f),
    ColorRGB(0, 0.75f, 0.75f),

    ColorRGB(0.5f, 0, 0)
  )
}
