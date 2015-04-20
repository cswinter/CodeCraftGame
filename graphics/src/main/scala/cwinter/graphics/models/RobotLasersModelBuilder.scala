package cwinter.graphics.models

import cwinter.graphics.engine.RenderStack
import cwinter.graphics.model._
import RobotColors._


case class RobotLasersModelBuilder(position: VertexXY, n: Int)(implicit rs: RenderStack)
  extends ModelBuilder[RobotLasersModelBuilder, Unit] {
  override def signature: RobotLasersModelBuilder = this

  override protected def buildModel: Model[Unit] = {
    Polygon(
      rs.MaterialXYRGB,
      3,
      Seq.fill(n)(White) ++ Seq.fill(3 - n)(Black),
      Seq.fill(3)(ColorThrusters),
      radius = 8,
      position = position,
      zPos = 1,
      orientation = 0
    ).getModel
  }
}
