package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import RobotColors._


case class RobotEnginesModel(position: VertexXY, t: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotEnginesModel, Unit] {

  def signature: RobotEnginesModel = this

  protected def buildModel: Model[Unit] = {
    val enginePositions = Geometry.polygonVertices2(3, radius = 5, orientation = 2 * math.Pi.toFloat * t / 250)
    val engines =
      for ((offset, i) <- enginePositions.zipWithIndex)
      yield Polygon(
        rs.MaterialXYRGB,
        5,
        ColorThrusters,
        ColorHull,
        radius = 4,
        position = position + offset,
        orientation = -2 * math.Pi.toFloat * t / 125,
        zPos = 1
      ).getModel

    new StaticCompositeModel(engines)
  }
}
