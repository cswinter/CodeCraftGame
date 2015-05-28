package cwinter.codinggame.testai

import cwinter.codinggame.core.api.{DroneSpec, MineralCrystalHandle, DroneHandle, DroneController}
import cwinter.codinggame.util.maths.Vector2


class CheesyMothership extends DroneController {
  override def onSpawn(): Unit = {
    buildDrone(new DroneSpec(5, 0, 3, 0, 0, 0, 1), new CheesyDestroyer(-position))
  }
  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()
  override def onTick(): Unit = ()
  override def onArrival(): Unit = ()
  override def onDeath(): Unit = ()
  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
}

class CheesyDestroyer(targetPos: Vector2) extends DroneController {
  override def onSpawn(): Unit = {
    moveToPosition(targetPos)
  }
  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()
  override def onTick(): Unit = {
    for (d <- dronesInSight.find(d => d.isEnemy && isInMissileRange(d))) {
      shootMissiles(d)
    }
  }
  override def onArrival(): Unit = ()
  override def onDeath(): Unit = ()
  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
}
