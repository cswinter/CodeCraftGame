package cwinter.codecraft.core.api

/**
  * In addition to your [[DroneController]]s you can have one [[MetaController]], which has a method
  * that will be called once every tick before the onEvent methods on your drone controllers are called.
  * This can be useful if you want to perform some global computation once every timestep.
  */
trait MetaController {
  def onTick(): Unit
}
