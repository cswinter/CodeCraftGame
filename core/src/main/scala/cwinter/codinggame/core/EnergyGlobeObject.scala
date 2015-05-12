package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.{EnergyGlobeDescriptor, WorldObjectDescriptor}

class EnergyGlobeObject(
  var position: Vector2,
  var tta: Int,
  targetPosition: Vector2
) extends WorldObject {
  val velocity = (targetPosition - position) / tta

  override private[core] def descriptor: Seq[WorldObjectDescriptor] = Seq(
    EnergyGlobeDescriptor(position.x.toFloat, position.y.toFloat)
  )

  override def update(): Seq[SimulatorEvent] = {
    tta -= 1
    position += velocity

    if (tta == 0) Seq(RemoveEnergyGlobeAnimation(this))
    else Seq()
  }

  override private[core] def hasDied: Boolean = tta <= 0
}
