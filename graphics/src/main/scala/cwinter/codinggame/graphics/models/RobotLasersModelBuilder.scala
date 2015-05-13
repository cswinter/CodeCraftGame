package cwinter.codinggame.graphics.models

import cwinter.codinggame.graphics.engine.RenderStack
import cwinter.codinggame.graphics.model.{Model, ModelBuilder, SquarePrimitive, StaticCompositeModel}
import cwinter.codinggame.graphics.models.DroneColors._
import cwinter.codinggame.util.maths.VertexXY
import cwinter.codinggame.worldstate.Player


case class DroneLasersModelBuilder(player: Player, position: VertexXY, n: Int)(implicit rs: RenderStack)
  extends ModelBuilder[DroneLasersModelBuilder, Unit] {
  override def signature: DroneLasersModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    val components =
      for {
        pos <- Seq(
          VertexXY(1, 1), VertexXY(0, 1), VertexXY(-1, 1),
          VertexXY(1, 0), VertexXY(0, 0), VertexXY(-1, 0),
          VertexXY(1, -1), VertexXY(0, -1), VertexXY(-1, -1))
        offset = pos * 4
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
        2,
        player.color,
        1
      ).getModel

    val element =
      SquarePrimitive(
        rs.BloomShader,
        midpoint.x,
        midpoint.y,
        1,
        White,
        2
      ).getModel

    Seq(background, element)
  }
}
