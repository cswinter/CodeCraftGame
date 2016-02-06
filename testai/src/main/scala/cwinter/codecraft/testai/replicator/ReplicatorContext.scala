package cwinter.codecraft.testai.replicator

import cwinter.codecraft.util.maths.Rectangle


class ReplicatorContext  {
  val harvestCoordinator = new HarvestCoordinator
  val battleCoordinator = new BattleCoordinator
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
}

