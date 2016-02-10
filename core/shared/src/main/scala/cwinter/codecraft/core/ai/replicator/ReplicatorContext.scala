package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat.BattleCoordinator
import cwinter.codecraft.core.ai.shared.{HarvestCoordinatorWithZones, SharedContext}


class ReplicatorContext extends SharedContext {
  val battleCoordinator = new BattleCoordinator
  val mothershipCoordinator = new MothershipCoordinator
  val harvestCoordinator = new HarvestCoordinatorWithZones
  var isReplicatorInConstruction: Boolean = false

  override def onTick(): Unit = {
    super.onTick()
    battleCoordinator.update()
  }
}

