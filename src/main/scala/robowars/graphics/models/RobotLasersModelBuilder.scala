package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model._
import RobotColors._


case class RobotLasersModelBuilder(position: VertexXY, n: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotLasersModelBuilder, Unit] {
  override def signature: RobotLasersModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    Polygon(
      rs.MaterialXYRGB,
      3,
      White,
      ColorThrusters,
      radius = 8,
      position = position,
      zPos = 1
    ).getModel
  }
}
