package cwinter.codinggame.graphics.models

import cwinter.codinggame.graphics.engine.RenderStack
import cwinter.codinggame.graphics.model.{Model, ModelBuilder, Polygon, StaticCompositeModel}
import cwinter.codinggame.graphics.models.DroneColors._
import cwinter.codinggame.util.maths.{Geometry, VertexXY}
import cwinter.codinggame.worldstate.Player


case class DroneEnginesModel(position: VertexXY, player: Player, t: Int)(implicit rs: RenderStack)
  extends ModelBuilder[DroneEnginesModel, Unit] {

  def signature: DroneEnginesModel = this

  protected def buildModel: Model[Unit] = {
    val enginePositions = Geometry.polygonVertices2(3, radius = 5, orientation = 2 * math.Pi.toFloat * t / 100)
    val engines =
      for ((offset, i) <- enginePositions.zipWithIndex)
      yield Polygon(
        rs.MaterialXYRGB,
        5,
        player.color,
        ColorHull,
        radius = 4,
        position = position + offset,
        orientation = -2 * math.Pi.toFloat * t / 125,
        zPos = 1
      ).getModel

    new StaticCompositeModel(engines)
  }
}
