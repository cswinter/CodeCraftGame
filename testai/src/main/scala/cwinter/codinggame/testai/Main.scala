package cwinter.codinggame.testai

import cwinter.codinggame.core._
import cwinter.codinggame.core.drone._
import cwinter.codinggame.util.maths.{Rng, Vector2}
import cwinter.codinggame.util.modules.ModulePosition
import cwinter.worldstate.BluePlayer

object Main {
  def main(args: Array[String]): Unit = {
    TheGameMaster.runLevel1(new Mothership)
  }

  private def events(t: Int): Seq[SimulatorEvent] = {
    if (t % 20 == 0) {
      Seq(SpawnDrone(randomAttackDrone))
    } else Seq()
  }

  private def randomAttackDrone: Drone = {
    val size = Rng.int(3, 6)
    new Drone(
      Seq.fill(ModulePosition.moduleCount(size))(Lasers), size,
      new AttackDroneController, BluePlayer,
      Rng.vector2(-1400, 1400, -1000, 1000),
      0, 0
    )
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
    if (!isConstructing) {
      if (collectors < 2) {
        buildSmallDrone(StorageModule, StorageModule, new ScoutingDroneController(this))
        collectors += 1
      } else {
        if (Rng.bernoulli(0.7)) {
          buildMediumDrone(Lasers, Lasers, ShieldGenerator, Engines, new AttackDroneController())
        } else {
          buildLargeDrone(ShieldGenerator, Engines, Engines, Lasers, Lasers, Lasers, Lasers, new AttackDroneController())
        }
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
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

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

