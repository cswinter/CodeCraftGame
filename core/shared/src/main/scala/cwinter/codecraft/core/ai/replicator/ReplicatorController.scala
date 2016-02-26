package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat.ReplicatorCommand
import cwinter.codecraft.core.ai.shared.AugmentedController
import cwinter.codecraft.core.api.Drone


class ReplicatorController(_context: ReplicatorContext)
extends AugmentedController[ReplicatorCommand, ReplicatorContext](_context) {
  override def onDroneEntersVision(drone: Drone): Unit = {
    super.onDroneEntersVision(drone)
    if (drone.isEnemy && drone.spec.missileBatteries > 0)
      context.battleCoordinator.foundArmedEnemy(drone)
  }


  def normalizedEnemyCount: Double =
    Util.approximateStrength(armedEnemies)
}

