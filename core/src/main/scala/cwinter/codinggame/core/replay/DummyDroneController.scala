package cwinter.codinggame.core.replay

import cwinter.codinggame.core.api.{MineralCrystalHandle, DroneHandle, DroneController}

class DummyDroneController extends DroneController {
  override def onSpawn(): Unit = ()
  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()
  override def onTick(): Unit = ()
  override def onArrival(): Unit = ()
  override def onDeath(): Unit = ()
  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
}
