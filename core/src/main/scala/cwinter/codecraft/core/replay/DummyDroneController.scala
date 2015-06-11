package cwinter.codecraft.core.replay

import cwinter.codecraft.core.api.{MineralCrystalHandle, DroneHandle, DroneController}

class DummyDroneController extends DroneController {
  override def onSpawn(): Unit = ()
  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()
  override def onTick(): Unit = ()
  override def onArrivesAtPosition(): Unit = ()
  override def onDeath(): Unit = ()
  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
}