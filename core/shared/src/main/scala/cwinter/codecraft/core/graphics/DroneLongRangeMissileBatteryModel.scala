package cwinter.codecraft.core.graphics

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.model.{CompositeModelBuilder, ModelBuilder}
import cwinter.codecraft.graphics.primitives.SquarePrimitive
import cwinter.codecraft.util.maths.{ColorRGB, VertexXY}

private[codecraft] case class DroneLongRangeMissileBatteryModel(
  colors: DroneColors,
  playerColor: ColorRGB,
  position: VertexXY,
  chargeup: Int
)(implicit rs: RenderStack)
    extends CompositeModelBuilder[DroneLongRangeMissileBatteryModel, Unit] {
  override def signature: DroneLongRangeMissileBatteryModel = this

  override protected def buildSubcomponents: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, Unit]]) = {
    val background =
      SquarePrimitive(
        rs.MaterialXYZRGB,
        position.x,
        position.y,
        5.0f,
        playerColor,
        1
      )

    val element =
      SquarePrimitive(
        rs.BloomShader,
        position.x,
        position.y,
        if (chargeup > 0) {
          1.0f + 0.1f * chargeup
        } else {
          0.1f
        },
        colors.White,
        2
      )
    (Seq(background, element), Seq.empty)
  }
}
