package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.{SimulatorEvent, SpawnHomingMissile}
import cwinter.codecraft.util.maths.Vector2
import cwinter.codecraft.worldstate.{DroneModuleDescriptor, MissileBatteryDescriptor}

private[core] class DroneMissileBatteryModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {
  import DroneConstants._

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
    if ((target.position - owner.position).size > MissileLockOnRadius) {
      owner.warn(s"Cannot fire homing missiles unless the target is within lock-on range ($MissileLockOnRadius)")
    } else {
      if (_cooldown <= 0) {
        _cooldown = 30

        val missiles =
          for (pos <- absoluteModulePositions)
            yield SpawnHomingMissile(owner.player, pos, target)

        nextEffect = (missiles, Seq.empty[Vector2], Seq.empty[Vector2])
      }
    }
  }

  override def descriptors: Seq[DroneModuleDescriptor] = positions.map(MissileBatteryDescriptor(_))
}

