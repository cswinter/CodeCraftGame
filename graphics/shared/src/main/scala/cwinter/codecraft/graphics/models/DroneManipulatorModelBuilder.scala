package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{ModelBuilder, Model, Polygon}
import cwinter.codecraft.graphics.worldstate.Player
import cwinter.codecraft.util.maths.{ColorRGBA, VertexXY}
import DroneColors._


case class DroneManipulatorModelBuilder(player: Player, position: VertexXY)(implicit rs: RenderStack)
  extends ModelBuilder[DroneManipulatorModelBuilder, Unit] {
  override def signature: DroneManipulatorModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    Polygon(
      rs.GaussianGlow,
      20,
      ColorRGBA(0.5f * player.color + 0.5f * White, 1),
      ColorRGBA(White, 0),
      radius = 8,
      position = position,
      zPos = 1,
      orientation = 0,
      colorEdges = true
    ).getModel
  }
}
