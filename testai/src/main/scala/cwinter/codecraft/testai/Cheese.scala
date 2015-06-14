package cwinter.codecraft.testai

import cwinter.codecraft.core.api.{DroneController, DroneHandle, DroneSpec, MineralCrystalHandle}
import cwinter.codecraft.util.maths.Vector2


class CheesyMothership extends DroneController {
  final val destroyerSpec = new DroneSpec(0, 3, 0, 0, 0, 1)

  override def onSpawn(): Unit = {
    buildDrone(destroyerSpec, new CheesyDestroyer(-position))
  }
  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()
  override def onTick(): Unit = ()
  override def onArrivesAtPosition(): Unit = ()
  override def onDeath(): Unit = ()
  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
}

class CheesyDestroyer(targetPos: Vector2) extends DroneController {
  override def onSpawn(): Unit = {
    moveTo(targetPos)
  }
  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()
  override def onTick(): Unit = {
    for (d <- dronesInSight.find(d => d.isEnemy && isInMissileRange(d))) {
      shootMissiles(d)
    }
  }
  override def onArrivesAtPosition(): Unit = ()
  override def onDeath(): Unit = ()
  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
}
