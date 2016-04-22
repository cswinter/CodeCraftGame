package cwinter.codecraft.core.objects

import cwinter.codecraft.core.graphics.{EnergyGlobeModel, PlainEnergyGlobeModel}
import cwinter.codecraft.core.objects.drone.DroneImpl
import cwinter.codecraft.core.{RemoveEnergyGlobeAnimation, SimulatorEvent}
import cwinter.codecraft.graphics.engine.{ModelDescriptor, PositionDescriptor}
import cwinter.codecraft.util.maths.Vector2


private[core] class EnergyGlobeObject(
  val frameOfReference: DroneImpl,
  var position: Vector2,
  var tta: Int,
  targetPosition: Vector2
) extends WorldObject {
  final val FadeTime = 15
  val velocity = (targetPosition - position) / tta
  var fade = FadeTime
  val id = -1

  override private[core] def descriptor: Seq[ModelDescriptor[_]] = {
    val dronePos = frameOfReference.position
    val sin = math.sin(frameOfReference.dynamics.orientation)
    val cos = math.cos(frameOfReference.dynamics.orientation)
    val x = cos * position.x - sin *  position.y
    val y = sin * position.x + cos *  position.y
    Seq(
      ModelDescriptor(
        PositionDescriptor(
          (x + dronePos.x).toFloat,
          (y + dronePos.y).toFloat, 0),
        if (tta > 0) PlainEnergyGlobeModel
        else EnergyGlobeModel(fade / FadeTime.toFloat)
      )
    )
  }

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

