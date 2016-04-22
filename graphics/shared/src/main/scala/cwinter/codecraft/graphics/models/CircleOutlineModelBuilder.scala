package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.WorldObjectDescriptor
import cwinter.codecraft.graphics.model.{Model, ModelBuilder}
import cwinter.codecraft.graphics.primitives.PolygonRing
import cwinter.codecraft.util.maths.{ColorRGB, VertexXY}


private[codecraft] case class CircleOutlineModelBuilder(
  radius: Float,
  color: ColorRGB = ColorRGB(1, 1, 1)
) extends ModelBuilder[CircleOutlineModelBuilder, Unit] with WorldObjectDescriptor[Unit] {

  override protected def buildModel: Model[Unit] =
    new PolygonRing(
      rs.MaterialXYZRGB, 40, Seq.fill(40)(color), Seq.fill(40)(color),
      radius - 2, radius, VertexXY(0, 0), 0, 0
    ).noCaching.getModel


  override def signature = this
  override def isCacheable = false
  override def allowCaching = false
}

