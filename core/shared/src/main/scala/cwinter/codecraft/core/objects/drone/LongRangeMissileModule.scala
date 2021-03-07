package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.GameConstants.{LongRangeMissileChargeup, LongRangeMissileLockOnRange}
import cwinter.codecraft.core.game.{SimulatorEvent, SpawnLongRangeHomingMissile}
import cwinter.codecraft.core.graphics.{DroneModuleDescriptor, LongRangeMissileBatteryDescriptor}
import cwinter.codecraft.util.maths.Vector2

private[core] class LongRangeMissileModule(positions: Seq[Int], owner: DroneImpl)
    extends DroneModule(positions, owner) {

  private[this] var _chargeup = 0
  private[this] var target: Option[DroneImpl] = None

  def chargeup: Int = _chargeup

  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var effect = NoEffects
    target.foreach(t => {
      if (t.isDead || (t.position - owner.position).lengthSquared > LongRangeMissileLockOnRange * LongRangeMissileLockOnRange) {
        target = None
        _chargeup = 0
        owner.invalidateModelCache()
      } else {
        _chargeup += 1
        if (_chargeup % 3 == 1) {
          owner.invalidateModelCache()
        }
        if (_chargeup == LongRangeMissileChargeup) {
          _chargeup = 0
          val missiles =
            for (pos <- absoluteModulePositions)
              yield
                SpawnLongRangeHomingMissile(owner.player,
                                            pos,
                                            owner.context.idGenerator.getAndIncrement(),
                                            t)
          effect = (missiles, Seq.empty[Vector2], Seq.empty[Vector2])
          owner.invalidateModelCache()
        }
      }
    })

    effect
  }

  def fire(target: DroneImpl): Unit = {
    if ((target.position - owner.position).length > LongRangeMissileLockOnRange) {
      owner.warn(
        s"Cannot fire homing missiles unless the target is within lock-on range ($LongRangeMissileLockOnRange)")
    } else {
      this.target = Some(target)
      _chargeup = 0
      owner.invalidateModelCache()
    }
  }

  override def descriptors: Seq[DroneModuleDescriptor] =
    positions.map(LongRangeMissileBatteryDescriptor(_, (2 + _chargeup) / 3))
}
