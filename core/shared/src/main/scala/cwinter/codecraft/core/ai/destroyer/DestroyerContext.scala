package cwinter.codecraft.core.ai.destroyer

import cwinter.codecraft.core.ai.shared.{BasicHarvestCoordinator, SharedContext}
import cwinter.codecraft.util.maths.Rectangle


class DestroyerContext extends SharedContext {
  val harvestCoordinator = new BasicHarvestCoordinator
  private var _mothership: Mothership = null
  def mothership: Mothership = _mothership


  def initialise(worldSize: Rectangle, mothership: Mothership): Unit = {
    initialise(worldSize)
    _mothership = mothership
  }
}
