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
    val components =
      for {
        pos <- Seq(VertexXY(1, 1), VertexXY(-1, 1), VertexXY(-1, -1), VertexXY(1, -1))
        offset = pos * 3
        segment <- buildSegment(offset + position)
      } yield segment

    new StaticCompositeModel(components)
  }


  def buildSegment(midpoint: VertexXY): Seq[Model[Unit]] = {
    val background =
      SquarePrimitive(
        rs.MaterialXYRGB,
        midpoint.x,
        midpoint.y,
        3,
        player.color,
        1
      ).getModel

    val element =
      SquarePrimitive(
        rs.BloomShader,
        midpoint.x,
        midpoint.y,
        2,
        White,
        2
      ).getModel

    Seq(background, element)
  }
}
