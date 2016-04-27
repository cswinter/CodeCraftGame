package cwinter.codecraft.core.objects

import cwinter.codecraft.core.graphics.LightFlashModel
import cwinter.codecraft.core.{LightFlashDestroyed, SimulatorEvent}
import cwinter.codecraft.graphics.engine.{ModelDescriptor, PositionDescriptor}
import cwinter.codecraft.util.maths.Vector2


private[core] class LightFlash(val position: Vector2) extends WorldObject {
  var stage: Float = 0
  val id = -1
  private val positionDescriptor =
    PositionDescriptor(position.x.toFloat, position.y.toFloat)

  override private[core] def descriptor: Seq[ModelDescriptor[_]] = Seq(
    ModelDescriptor(
      positionDescriptor,
      LightFlashModel,
      stage
    )
  )

  def update(): Seq[SimulatorEvent] = {
    stage += 1.0f / 10

    if (stage > 1) {
      Seq(LightFlashDestroyed(this))
    } else Seq.empty[SimulatorEvent]
  }

  def isDead: Boolean = stage > 1
}
