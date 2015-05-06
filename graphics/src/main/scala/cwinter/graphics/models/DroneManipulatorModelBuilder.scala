package cwinter.graphics.models

import cwinter.codinggame.util.maths.{ColorRGBA, VertexXY}
import cwinter.graphics.engine.RenderStack
import cwinter.graphics.model._
import cwinter.graphics.models.RobotColors._
import cwinter.worldstate.Player


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
