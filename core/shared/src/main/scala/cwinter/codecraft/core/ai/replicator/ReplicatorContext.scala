package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat.BattleCoordinator
import cwinter.codecraft.core.api.MetaController
import cwinter.codecraft.util.maths.Rectangle

import scala.util.Random


class ReplicatorContext extends MetaController {
  val rng = new Random()
  val harvestCoordinator = new HarvestCoordinator
  val battleCoordinator = new BattleCoordinator
  val mothershipCoordinator = new MothershipCoordinator
  val droneCount = new DroneCounter
  private[this] var _searchCoordinator: SearchCoordinator = null
  def searchCoordinator = {
    require(_searchCoordinator != null, "Context is uninitialised.")
    _searchCoordinator
  }



  def initialise(worldSize: Rectangle): Unit = {
    if (initialisationRequired) {
      _searchCoordinator = new SearchCoordinator(worldSize)
    }
  }

  def initialisationRequired: Boolean = _searchCoordinator == null

  override def onTick(): Unit = {
    harvestCoordinator.update()
    battleCoordinator.update()
  }
}

