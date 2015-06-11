package cwinter.codecraft.core.objects

import cwinter.codecraft.core.{LightFlashDestroyed, SimulatorEvent}
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.worldstate.{LightFlashDescriptor, WorldObjectDescriptor}


private[core] class LightFlash(val position: Vector2) extends WorldObject {
  var stage: Float = 0

  override private[core] def descriptor: Seq[WorldObjectDescriptor] = Seq(
    LightFlashDescriptor(id, position.x.toFloat, position.y.toFloat, stage)
  )

  def update(): Seq[SimulatorEvent] = {
    stage += 1.0f / 5

    if (stage > 1) {
      Seq(LightFlashDestroyed(this))
    } else Seq.empty[SimulatorEvent]
  }

  def hasDied: Boolean = stage > 1
}