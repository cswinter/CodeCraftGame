package cwinter.codecraft.core.ai.cheese

import cwinter.codecraft.core.api.{Drone, DroneController, DroneSpec, MineralCrystal}


private[core] class Mothership extends DroneController {
  final val destroyerSpec = new DroneSpec(0, 3, 0, 0, 1)

  override def onSpawn(): Unit = {
    buildDrone(new Destroyer(-position), destroyerSpec)
    moveTo(-position)
  }
  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onTick(): Unit = ()
  override def onArrivesAtPosition(): Unit = ()
  override def onDeath(): Unit = ()
  override def onDroneEntersVision(drone: Drone): Unit = ()
}


