package cwinter.codinggame.core.drone

import cwinter.codinggame.core.{SimulatorEvent, SpawnLaserMissile}

class DroneLasersModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {
  final val LockOnRadius = 300

  private[this] var nextEffect = NoEffects
  private[this] var _cooldown = 0

  def cooldown: Int = _cooldown


  override def update(availableResources: Int): (Seq[SimulatorEvent], Int) = {
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
        _cooldown = 30

        val missiles =
          for (pos <- absoluteModulePositions)
            yield SpawnLaserMissile(owner.player, pos, target)

        nextEffect = (missiles, 0)
      }
    }
  }

}

