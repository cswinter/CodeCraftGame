package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.WorldObjectDescriptor
import cwinter.codecraft.graphics.model.{Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.util.maths.{ColorRGB, Rectangle, VertexXY}


private[codecraft] case class MineralCrystalModel(
  size: Int,
  xPos: Float,
  yPos: Float,
  orientation: Float
) extends ModelBuilder[MineralCrystalModel, Unit] with WorldObjectDescriptor[Unit] {

  override protected def buildModel: Model[Unit] = {
    val size = signature.size
    val radius = math.sqrt(size).toFloat * 3

    Polygon(
      rs.BloomShader,
      n = 5,
      colorMidpoint = ColorRGB(0.03f, 0.6f, 0.03f),
      colorOutside = ColorRGB(0.0f, 0.1f, 0.0f),
      radius = radius,
      zPos = -5,
      position = VertexXY(xPos, yPos),
      orientation = orientation
    ).getModel
  }

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean =
    intersects(this.xPos, this.yPos, 50, rectangle)
  override def signature = this
}

