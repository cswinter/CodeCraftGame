package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.graphics.worldstate.MineralDescriptor
import cwinter.codecraft.util.maths.{VertexXY, ColorRGB}


private[graphics] class MineralModelBuilder(
  mineral: MineralDescriptor
)(implicit val rs: RenderStack)
  extends ModelBuilder[MineralDescriptor, Unit] {

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
      position = VertexXY(mineral.xPos, mineral.yPos),
      orientation = mineral.orientation
    ).getModel
  }

  def signature = mineral
}
