package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import RobotColors._


case class RobotStorageModule(position: VertexXY, nEnergyGlobes: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotStorageModule, Unit] {


  def signature = this

  protected def buildModel: Model[Unit] = {
    val radius = 8
    val outlineWidth = 1
    val body =
      Polygon(
        material = rs.MaterialXYRGB,
        n = 20,
        colorMidpoint = ColorBackplane,
        colorOutside = ColorBackplane,
        radius = radius - outlineWidth,
        position = position,
        zPos = 1
      ).getModel

    val hull =
      PolygonRing(
        material = rs.MaterialXYRGB,
        n = 20,
        colorInside = ColorHull,
        colorOutside = ColorHull,
        innerRadius = radius - outlineWidth,
        outerRadius = radius,
        position = position,
        zPos = 1
      ).getModel

    val energyPositions = Seq(VertexXY(0, 0)) ++ Geometry.polygonVertices2(6, radius = 4.5f)
    val energyGlobes =
      for (i <- 0 until nEnergyGlobes)
      yield
        Polygon(
          material = rs.BloomShader,
          n = 7,
          colorMidpoint = ColorRGB(1, 1, 1),
          colorOutside = ColorRGB(0, 1, 0),
          radius = 2,
          position = energyPositions(i) + position,
          zPos = 2
        ).getModel

    new StaticCompositeModel(body +: hull +: energyGlobes)
  }
}
