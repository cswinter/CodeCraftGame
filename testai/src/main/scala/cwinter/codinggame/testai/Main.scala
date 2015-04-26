package cwinter.codinggame.testai

import cwinter.codinggame.core.{StorageModule, MineralCrystal, DroneController, TheGameMaster}
import cwinter.codinggame.util.maths.{Rng, Vector2}

object Main {
  def main(args: Array[String]): Unit = {
    TheGameMaster.startGame(new Mothership)
  }
}


class Mothership extends DroneController {
  var t = 0

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildSmallDrone(StorageModule, StorageModule, new ScoutingDroneController(this))
  }

  override def onTick(): Unit = {
    if (availableFactories >= 4) {
     buildSmallDrone(StorageModule, StorageModule, new ScoutingDroneController(this))
    } else if (availableFactories >= 2) {
     buildTinyDrone(StorageModule, new ScoutingDroneController(this))
    }
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onArrival(): Unit = ()
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
      for (
        mineral <- nextCrystal
        if mineral.position ~ position
      ) {
        harvestMineral(mineral)
        nextCrystal = None
      }
    }
  }
}

