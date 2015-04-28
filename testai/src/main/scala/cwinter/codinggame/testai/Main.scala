package cwinter.codinggame.testai

import cwinter.codinggame.core._
import cwinter.codinggame.util.maths.{Rng, Vector2}

object Main {
  def main(args: Array[String]): Unit = {
    TheGameMaster.startGame(new Mothership)
  }
}


class Mothership extends DroneController {
  var t = 0
  var collectors = 0

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildTinyDrone(StorageModule, new ScoutingDroneController(this))
  }

  override def onTick(): Unit = {
    if (availableFactories >= 4) {
      if (collectors < 2) {
        buildSmallDrone(StorageModule, StorageModule, new ScoutingDroneController(this))
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
}

class ScoutingDroneController(val mothership: Mothership) extends DroneController {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystal] = None


  // abstract methods for event handling
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = {
    if (mineralCrystal.size <= availableStorage) {
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
    if (weaponsCooldown == 0 && dronesInSight.nonEmpty) {
      shootWeapons(dronesInSight.head)
    }
    if (Rng.bernoulli(0.01)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  override def onArrival(): Unit = ()

  override def onDroneEntersVision(drone: Drone): Unit = {
    println("Shooting weapons!")
    shootWeapons(drone)
  }
}

