package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.model.{Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}


private[codecraft] case class BasicHomingMissileModel(
  x: Float,
  y: Float,
  playerColor: ColorRGB
) extends ModelBuilder[BasicHomingMissileModel, Unit] with WorldObjectDescriptor[Unit] {

  def buildModel: Model[Unit] = {
    Polygon(
      material = rs.TranslucentAdditive,
      n = 10,
      colorMidpoint = ColorRGBA(1, 1, 1, 1),
      colorOutside = ColorRGBA(playerColor, 1),
      radius = 5,
      position = VertexXY(x, y),
      zPos = 3
    ).noCaching.getModel
  }


  override protected def createModel(timestep: Int) = getModel
  override def signature = this
}