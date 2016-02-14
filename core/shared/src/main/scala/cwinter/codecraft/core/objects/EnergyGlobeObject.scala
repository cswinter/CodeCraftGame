package cwinter.codecraft.core.objects

import cwinter.codecraft.core.{RemoveEnergyGlobeAnimation, SimulatorEvent}
import cwinter.codecraft.graphics.worldstate.{PlainEnergyGlobeDescriptor, EnergyGlobeDescriptor, ModelDescriptor, PositionDescriptor}
import cwinter.codecraft.util.maths.Vector2

private[core] class EnergyGlobeObject(
  var position: Vector2,
  var tta: Int,
  targetPosition: Vector2
) extends WorldObject {
  final val FadeTime = 15
  val velocity = (targetPosition - position) / tta
  var fade = FadeTime
  val id = -1

  override private[core] def descriptor: Seq[ModelDescriptor] = Seq(
    ModelDescriptor(
      PositionDescriptor(position.x.toFloat, position.y.toFloat, 0),
      if (tta > 0) PlainEnergyGlobeDescriptor
      else EnergyGlobeDescriptor(fade / FadeTime.toFloat)
    )
  )

  override def update(): Seq[SimulatorEvent] = {
    if (tta > 0) {
      tta -= 1
      position += velocity
    } else {
      fade -= 1
    }
    if (fade == 0) Seq(RemoveEnergyGlobeAnimation(this))
    else Seq()
  }

  override private[core] def isDead: Boolean = fade <= 0
}

