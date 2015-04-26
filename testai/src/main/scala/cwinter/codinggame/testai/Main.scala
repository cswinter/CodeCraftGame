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
    buildSmallDrone(StorageModule, StorageModule, new ScoutingDroneController())
  }

  override def onTick(): Unit = {
    if (availableFactories >= 4) {
     buildSmallDrone(StorageModule, StorageModule, new ScoutingDroneController())
    } else if (availableFactories >= 2) {
     buildTinyDrone(StorageModule, new ScoutingDroneController())
    }
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onArrival(): Unit = ()
}

class ScoutingDroneController extends DroneController {
  // abstract methods for event handling
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = {
    
  }

  override def onTick(): Unit = {
    if (Rng.bernoulli(0.005)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  override def onArrival(): Unit = { }
}
