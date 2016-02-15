package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{Model, ModelBuilder, StaticCompositeModel}
import cwinter.codecraft.graphics.primitives.SquarePrimitive
import cwinter.codecraft.util.maths.{ColorRGB, VertexXY}


private[graphics] case class DroneMissileBatteryModelBuilder(
  colors: DroneColors,
  playerColor: ColorRGB,
  position: VertexXY,
  n: Int
)(implicit rs: RenderStack) extends ModelBuilder[DroneMissileBatteryModelBuilder, Unit] {
  override def signature: DroneMissileBatteryModelBuilder = this

  override protected def buildModel: Model[Unit] = {
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

    new StaticCompositeModel(components)
  }


  def buildSegment(midpoint: VertexXY): Seq[Model[Unit]] = {
    val background =
      SquarePrimitive(
        rs.MaterialXYZRGB,
        midpoint.x,
        midpoint.y,
        2.5f,
        playerColor,
        1
      ).getModel

    val element =
      SquarePrimitive(
        rs.BloomShader,
        midpoint.x,
        midpoint.y,
        1.5f,
        colors.White,
        2
      ).getModel

    Seq(background, element)
  }
}
