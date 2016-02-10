package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.MetaController
import cwinter.codecraft.util.maths.Rectangle

import scala.util.Random


trait SharedContext extends MetaController {
  val rng = new Random()
  val droneCount = new DroneCounter

  private[this] var _searchCoordinator: SearchCoordinator = null
  def searchCoordinator = {
    require(_searchCoordinator != null, "Context is uninitialised.")
    _searchCoordinator
  }

  val harvestCoordinator: BasicHarvestCoordinator


  def initialise(worldSize: Rectangle): Unit = {
    if (initialisationRequired) {
      _searchCoordinator = new SearchCoordinator(worldSize)
    }
  }

  def initialisationRequired: Boolean = _searchCoordinator == null

  override def onTick(): Unit = {
    harvestCoordinator.update()
  }
}

