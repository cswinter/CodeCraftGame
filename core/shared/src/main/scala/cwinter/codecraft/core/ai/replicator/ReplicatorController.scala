package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.shared.AugmentedController
import cwinter.codecraft.core.api.Drone


class ReplicatorController(
  _name: Symbol,
  _context: ReplicatorContext
) extends AugmentedController(_name, _context) {
  override def onDroneEntersVision(drone: Drone): Unit = {
    super.onDroneEntersVision(drone)
    if (drone.isEnemy && drone.spec.constructors > 0) {
      context.battleCoordinator.foundCapitalShip(drone)
    }
  }
}
