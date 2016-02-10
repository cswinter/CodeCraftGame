package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.{BattleCoordinator, BasicHarvestCoordinator, SharedContext}
import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Rectangle


class DestroyerContext extends SharedContext[DestroyerCommand] {
  val harvestCoordinator = new BasicHarvestCoordinator
  override val battleCoordinator = new DestroyerBattleCoordinator
  private var _mothership: Mothership = null
  def mothership: Mothership = _mothership


  def initialise(worldSize: Rectangle, mothership: Mothership): Unit = {
    initialise(worldSize)
    _mothership = mothership
  }
}

trait DestroyerCommand

class DestroyerBattleCoordinator extends BattleCoordinator[DestroyerCommand] {
  def getTarget: Option[Drone] =
    if (enemyCapitalShips.isEmpty) None
    else Some(enemyCapitalShips.minBy(_.spec.missileBatteries))
}
