package cwinter.codecraft.core.replay

import cwinter.codecraft.core.api.{MineralCrystal, Drone, DroneController}

private[codecraft] class DummyDroneController extends DroneController {
  override def onSpawn(): Unit = ()
  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onTick(): Unit = ()
  override def onArrivesAtPosition(): Unit = ()
  override def onDeath(): Unit = ()
  override def onDroneEntersVision(drone: Drone): Unit = ()
}
