package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import RobotColors._


case class RobotLasersModelBuilder(position: VertexXY, n: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotLasersModelBuilder, Unit] {
  override def signature: RobotLasersModelBuilder = this

  override protected def buildModel: Model[Unit] = {

    val posCharges = Geometry.polygonVertices2(3, radius = 5)
    val charges =
      for (pos <- posCharges)
        yield Polygon(rs.MaterialXYRGB, 15, White, White, 1.5f, pos + position, zPos = 2).getModel


    new StaticCompositeModel(
      Seq(
        Polygon(
          rs.MaterialXYRGB,
          4,
          ColorRGB(0.65f, 0.65f, 0.75f),
          ColorRGB(0.65f, 0.65f, 0.75f),
          radius = 3,
          position = position,
          zPos = 1
        ).getModel
      ) ++ charges
    )
  }
}
