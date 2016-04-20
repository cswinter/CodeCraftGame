package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.model.ModelBuilder
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, NullVectorXY, Rectangle}


private[codecraft] case class EnergyGlobeModelBuilder(fade: Float)
  extends ModelBuilder[EnergyGlobeModelBuilder, Unit] with WorldObjectDescriptor[Unit] {
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
  override protected def createModel(timestep: Int) = getModel
  override def signature = this
}

private[codecraft] object PlainEnergyGlobeModelBuilder extends EnergyGlobeModelBuilder(1)


