package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import RobotColors._
import robowars.worldstate.MineralObject


case class RobotStorageModelBuilder(position: VertexXY, nEnergyGlobes: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotStorageModelBuilder, Unit] {


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
    val contents =
      if (nEnergyGlobes > 0) {
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
      } else if (nEnergyGlobes == -1) {
        Seq(
          Polygon(
            rs.MaterialXYRGB,
            n = 5,
            colorMidpoint = ColorRGB(0.1f, 0.75f, 0.1f),
            colorOutside = ColorRGB(0.0f, 0.3f, 0.0f),
            radius = 6.5f,
            zPos = 2,
            position = position
          ).getModel
        )
      } else Seq()

    new StaticCompositeModel(body +: hull +: contents)
  }
}
