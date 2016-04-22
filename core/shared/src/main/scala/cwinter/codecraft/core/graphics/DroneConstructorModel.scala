package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.primitives.Polygon
import cwinter.codecraft.util.maths.{ColorRGB, ColorRGBA, VertexXY}


private[graphics] case class DroneConstructorModel(
  colors: DroneColors,
  playerColor: ColorRGB,
  position: VertexXY
)(implicit rs: RenderStack) extends CompositeModelBuilder[DroneConstructorModel, Unit] {
  override def signature: DroneConstructorModel = this

  override protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Unit]]) = {
    val module = Polygon(
      rs.GaussianGlow,
      20,
      ColorRGBA(0.5f * playerColor + 0.5f * colors.White, 1),
      ColorRGBA(colors.White, 0),
      radius = 8,
      position = position,
      zPos = 1,
      orientation = 0,
      colorEdges = true
    )

    (Seq(module), Seq.empty)
  }
}

