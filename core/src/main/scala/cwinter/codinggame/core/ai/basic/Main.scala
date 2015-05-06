package cwinter.codinggame.core.ai.basic

import cwinter.codinggame.core._
import cwinter.codinggame.core.drone._
import cwinter.codinggame.util.maths.{Rng, Vector2}


class Mothership extends DroneController {
  var t = 0
  var collectors = 0

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildTinyDrone(StorageModule, new ScoutingDroneController(this))
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      if (collectors < 2) {
        buildSmallDrone(StorageModule, if (Rng.bernoulli(0.9f)) Engines else StorageModule, new ScoutingDroneController(this))
        collectors += 1
      } else {
        buildSmallDrone(Lasers, Lasers, new AttackDroneController())
      }
    } else {
      for (mineralCrystal <- storedMinerals) {
        if (availableFactories >= mineralCrystal.size) {
          processMineral(mineralCrystal)
        }
      }
    }
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onArrival(): Unit = ()
  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

class ScoutingDroneController(val mothership: Mothership) extends DroneController {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None


  // abstract methods for event handling
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onDeath(): Unit = mothership.collectors -= 1

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = {
    if (nextCrystal.isEmpty && mineralCrystal.size <= availableStorage) {
      moveToPosition(mineralCrystal.position)
      nextCrystal = Some(mineralCrystal)
    }
  }

  override def onTick(): Unit = {
    if (availableStorage == 0 && !hasReturned) {
      moveToDrone(mothership)
    } else if ((hasReturned && availableStorage > 0) || Rng.bernoulli(0.005) && nextCrystal == None) {
      hasReturned = false
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  override def onArrival(): Unit = {
    if (availableStorage == 0) {
      depositMineralCrystals(mothership)
      hasReturned = true
    } else {
      if (nextCrystal.map(_.harvested) == Some(true)) {
        nextCrystal = None
      }
      for (
        mineral <- nextCrystal
        if mineral.position ~ position
      ) {
        harvestMineral(mineral)
        nextCrystal = None
      }
    }
  }

  override def onDroneEntersVision(drone: Drone): Unit = ()
}

class AttackDroneController extends DroneController {
  // abstract methods for event handling
  override def onSpawn(): Unit = ()

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

  override def onTick(): Unit = {
    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = enemies.head
      shootWeapons(enemy)
      moveInDirection(enemy.position - position)
    }
    if (Rng.bernoulli(0.01)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  def enemies: Set[Drone] =
    dronesInSight.filter(_.player != drone.player)

  override def onArrival(): Unit = ()

  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

