package cwinter.codecraft.core.api

import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.util.maths.Rectangle

/** In addition to your [[DroneController]]s you can have one [[MetaController]], which has a method
  * that will be called once every tick before the onEvent methods on your drone controllers are called.
  * This can be useful if you want to perform some global computation once every timestep.
  * You can instantiate your [[MetaController]] using the [[DroneControllerBase.metaController]] method.
  */
trait MetaController {
  private[core] var _worldSize: Rectangle = _
  private[core] var _tickPeriod: Int = -1
  private[core] implicit var _simulationContext: SimulationContext = _

  def onTick(): Unit
  def gameOver(winner: Player): Unit = ()
  def init(): Unit = ()

  private[codecraft] def onTick(simulationContext: SimulationContext): Unit = {
    _simulationContext = simulationContext
    onTick()
  }

  def worldSize: Rectangle = {
    require(_worldSize != null, cantAccessYet("worldSize"))
    _worldSize
  }

  def tickPeriod: Int = {
    require(_tickPeriod != -1, cantAccessYet("tickPeriod"))
    _tickPeriod
  }

  private def cantAccessYet(property: String): String =
    s"`$property` is only available after the game has started. " +
      s"If you have any initialisation code that relies on `$property`, make it lazy or override the `init()` method and put it there."
}
