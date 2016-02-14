package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{ModelBuilder, Polygon}
import cwinter.codecraft.graphics.worldstate.EnergyGlobeDescriptor
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, NullVectorXY}


private[graphics] class EnergyGlobeModelBuilder(
  val signature: EnergyGlobeDescriptor
)(implicit val rs: RenderStack) extends ModelBuilder[EnergyGlobeDescriptor, Unit] {

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
}
