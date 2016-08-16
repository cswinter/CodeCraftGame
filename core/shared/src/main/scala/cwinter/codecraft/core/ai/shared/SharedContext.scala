package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.MetaController
import cwinter.codecraft.util.maths.{RNG, Rectangle}

import scala.util.Random


private[codecraft] trait SharedContext[TCommand] extends MetaController {
  val rng = new RNG(0)
  val droneCount = new DroneCounter

  private[this] lazy val _searchCoordinator: SearchCoordinator = new SearchCoordinator(worldSize)
  def searchCoordinator = {
    require(_searchCoordinator != null, "Context is uninitialised.")
    _searchCoordinator
  }

  def harvestCoordinator: BasicHarvestCoordinator
  def battleCoordinator: BattleCoordinator[TCommand]

  override def onTick(): Unit = {
    harvestCoordinator.update()
    battleCoordinator.update()
  }
}

