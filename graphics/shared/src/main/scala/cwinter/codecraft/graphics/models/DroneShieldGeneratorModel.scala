package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.{Polygon, PolygonRing}
import cwinter.codecraft.util.maths.Geometry._
import cwinter.codecraft.util.maths.{ColorRGB, VertexXY}


private[graphics] case class DroneShieldGeneratorModel(
  position: VertexXY,
  colors: DroneColors,
  playerColor: ColorRGB
)(implicit rs: RenderStack)
  extends CompositeModelBuilder[DroneShieldGeneratorModel, Unit] {
  def signature = this


  override protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Unit]]) = {
    val radius = 3
    val gridposRadius = 2 * inradius(radius, 6)
    val gridpoints = VertexXY(0, 0) +: polygonVertices(6, radius = gridposRadius)
    val hexgrid =
      for (pos <- gridpoints)
      yield
        PolygonRing(
          material = rs.MaterialXYZRGB,
          n = 6,
          colorInside = colors.White,
          colorOutside = colors.White,
          innerRadius = radius - 0.5f,
          outerRadius = radius,
          position = pos + position,
          zPos = 1
        )

    val filling =
      for (pos <- gridpoints)
      yield
        Polygon(
          material = rs.MaterialXYZRGB,
          n = 6,
          colorMidpoint = playerColor,
          colorOutside = playerColor,
          radius = radius - 0.5f,
          position = pos + position,
          zPos = 1
        )

    (hexgrid ++ filling, Seq.empty)
  }
}
