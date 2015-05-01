package cwinter.graphics.models

import cwinter.codinggame.util.maths.VertexXY
import cwinter.graphics.engine.RenderStack
import cwinter.graphics.model._
import RobotColors._
import cwinter.worldstate.Player


case class RobotLasersModelBuilder(player: Player, position: VertexXY, n: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotLasersModelBuilder, Unit] {
  override def signature: RobotLasersModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    Polygon(
      rs.MaterialXYRGB,
      3,
      Seq.fill(n)(White) ++ Seq.fill(3 - n)(player.color),
      Seq.fill(3)(0.3f * player.color),
      radius = 8,
      position = position,
      zPos = 1,
      orientation = 0,
      colorEdges = true
    ).getModel
  }
}
