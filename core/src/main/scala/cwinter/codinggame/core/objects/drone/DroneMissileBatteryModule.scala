package cwinter.codinggame.core.objects.drone

import cwinter.codinggame.core.{SimulatorEvent, SpawnHomingMissile}
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.{DroneModuleDescriptor, MissileBatteryDescriptor}

class DroneMissileBatteryModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {
  final val LockOnRadius = 300

  private[this] var nextEffect = NoEffects
  private[this] var _cooldown = 0

  def cooldown: Int = _cooldown


  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    if (_cooldown > 0) _cooldown = _cooldown - 1

    val result = nextEffect
    nextEffect = NoEffects
    result
  }


  def fire(target: Drone): Unit = {
    if ((target.position - owner.position).size > LockOnRadius) {
      // TODO: report error
    } else {
      if (_cooldown <= 0) {
        if (owner.spec.isMothership) _cooldown = 10
        else _cooldown = 30

        val missiles =
          for (pos <- absoluteModulePositions)
            yield SpawnHomingMissile(owner.player, pos, target)

        nextEffect = (missiles, Seq.empty[Vector2], Seq.empty[Vector2])
      }
    }
  }

  override def descriptors: Seq[DroneModuleDescriptor] = positions.map(MissileBatteryDescriptor(_))
}

