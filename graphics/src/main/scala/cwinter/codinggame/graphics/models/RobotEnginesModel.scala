package cwinter.codinggame.graphics.models

import cwinter.codinggame.graphics.engine.RenderStack
import cwinter.codinggame.graphics.model.{Model, ModelBuilder, Polygon, StaticCompositeModel}
import cwinter.codinggame.graphics.models.RobotColors._
import cwinter.codinggame.util.maths.{Geometry, VertexXY}


case class RobotEnginesModel(position: VertexXY, t: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotEnginesModel, Unit] {

  def signature: RobotEnginesModel = this

  protected def buildModel: Model[Unit] = {
    val enginePositions = Geometry.polygonVertices2(3, radius = 5, orientation = 2 * math.Pi.toFloat * t / 100)
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
