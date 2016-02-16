package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{CompositeModelBuilder, ModelBuilder}
import cwinter.codecraft.graphics.primitives.SquarePrimitive
import cwinter.codecraft.util.maths.{ColorRGB, VertexXY}


private[graphics] case class DroneMissileBatteryModelBuilder(
  colors: DroneColors,
  playerColor: ColorRGB,
  position: VertexXY,
  n: Int
)(implicit rs: RenderStack) extends CompositeModelBuilder[DroneMissileBatteryModelBuilder, Unit] {
  override def signature: DroneMissileBatteryModelBuilder = this


  override protected def build: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Unit]]) = {
    /*
    val positions = Seq(
          VertexXY(1, 1), VertexXY(0, 1), VertexXY(-1, 1),
          VertexXY(1, 0), VertexXY(0, 0), VertexXY(-1, 0),
          VertexXY(1, -1), VertexXY(0, -1), VertexXY(-1, -1))
          */
    val positions = Seq(VertexXY(1, 1), VertexXY(1, -1), VertexXY(-1, -1), VertexXY(-1, 1))

    val components =
      for {
        pos <- positions
        offset = pos * 3
        segment <- buildSegment(offset + position)
      } yield segment

    (components, Seq.empty)
  }


  def buildSegment(midpoint: VertexXY): Seq[ModelBuilder[_, Unit]] = {
    val background =
      SquarePrimitive(
        rs.MaterialXYZRGB,
        midpoint.x,
        midpoint.y,
        2.5f,
        playerColor,
        1
      )

    val element =
      SquarePrimitive(
        rs.BloomShader,
        midpoint.x,
        midpoint.y,
        1.5f,
        colors.White,
        2
      )

    Seq(background, element)
  }
}
