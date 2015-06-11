package cwinter.codecraft.core.objects

import cwinter.codecraft.core.{RemoveEnergyGlobeAnimation, SimulatorEvent}
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.worldstate.{EnergyGlobeDescriptor, WorldObjectDescriptor}

class EnergyGlobeObject(
  var position: Vector2,
  var tta: Int,
  targetPosition: Vector2
) extends WorldObject {
  final val FadeTime = 15
  val velocity = (targetPosition - position) / tta
  var fade = FadeTime

  override private[core] def descriptor: Seq[WorldObjectDescriptor] = Seq(
    if (tta > 0) {
      EnergyGlobeDescriptor(position.x.toFloat, position.y.toFloat)
    } else {
      EnergyGlobeDescriptor(position.x.toFloat, position.y.toFloat, fade / FadeTime.toFloat)
    }
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

  override private[core] def hasDied: Boolean = fade <= 0
}

