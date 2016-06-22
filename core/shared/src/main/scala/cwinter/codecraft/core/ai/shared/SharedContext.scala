package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.MetaController
import cwinter.codecraft.util.maths.{RNG, Rectangle}

import scala.util.Random


private[codecraft] trait SharedContext[TCommand] extends MetaController {
  val rng = new RNG(0)
  val droneCount = new DroneCounter

  private[this] var _searchCoordinator: SearchCoordinator = null
  def searchCoordinator = {
    require(_searchCoordinator != null, "Context is uninitialised.")
    _searchCoordinator
  }

  val harvestCoordinator: BasicHarvestCoordinator
  val battleCoordinator: BattleCoordinator[TCommand]


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

