package cwinter.codecraft.core.objects

import cwinter.codecraft.core.{LightFlashDestroyed, SimulatorEvent}
import cwinter.codecraft.graphics.worldstate.{ModelDescriptor, LightFlashDescriptor, WorldObjectDescriptor}
import cwinter.codecraft.util.maths.Vector2


private[core] class LightFlash(val position: Vector2) extends WorldObject {
  var stage: Float = 0
  val id = -1

  override private[core] def descriptor: Seq[ModelDescriptor] = Seq(
    ModelDescriptor(
      position.x.toFloat,
      position.y.toFloat,
      0,
      LightFlashDescriptor(stage)
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
