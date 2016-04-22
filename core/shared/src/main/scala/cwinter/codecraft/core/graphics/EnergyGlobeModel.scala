package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.WorldObjectDescriptor
import cwinter.codecraft.graphics.model.ModelBuilder
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, NullVectorXY, Rectangle}


private[codecraft] case class EnergyGlobeModel(fade: Float)
  extends ModelBuilder[EnergyGlobeModel, Unit] with WorldObjectDescriptor[Unit] {
  require(fade >= 0)
  require(fade <= 1)

  override protected def buildModel = {
    if (signature.fade == 1) {
      Polygon(
        material = rs.BloomShader,
        n = 7,
        colorMidpoint = ColorRGB(1, 1, 1),
        colorOutside = ColorRGB(0, 1, 0),
        radius = 2,
        position = NullVectorXY,
        zPos = 3
      )
    } else {
      Polygon(
        material = rs.TranslucentProportional,
        n = 7,
        colorMidpoint = ColorRGBA(1, 1, 1, signature.fade),
        colorOutside = ColorRGBA(0, 1, 0, signature.fade),
        radius = 2,
        position = NullVectorXY,
        zPos = 3
      )
    }
  }.getModel

  override def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean =
    intersects(xPos, yPos, 20, rectangle) // FIXME
  override def signature = this
}

private[codecraft] object PlainEnergyGlobeModel extends EnergyGlobeModel(1)


