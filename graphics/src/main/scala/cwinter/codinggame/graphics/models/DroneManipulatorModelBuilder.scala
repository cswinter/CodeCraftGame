package cwinter.codinggame.graphics.models

import cwinter.codinggame.graphics.engine.RenderStack
import cwinter.codinggame.graphics.model.{ModelBuilder, Model, Polygon}
import cwinter.codinggame.util.maths.{ColorRGBA, VertexXY}
import RobotColors._
import cwinter.codinggame.worldstate.Player


case class DroneManipulatorModelBuilder(player: Player, position: VertexXY)(implicit rs: RenderStack)
  extends ModelBuilder[DroneManipulatorModelBuilder, Unit] {
  override def signature: DroneManipulatorModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    Polygon(
      rs.GaussianGlow,
      20,
      ColorRGBA(0.5f * player.color + 0.5f * White, 0),
      ColorRGBA(White, 1),
      radius = 8,
      position = position,
      zPos = 1,
      orientation = 0,
      colorEdges = true
    ).getModel
  }
}
