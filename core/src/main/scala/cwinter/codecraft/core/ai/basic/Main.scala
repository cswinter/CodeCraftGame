package cwinter.codecraft.core.ai.basic

import cwinter.codecraft.core.api.{DroneController, DroneHandle, DroneSpec, MineralCrystalHandle}
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.core.objects.MineralCrystal
import cwinter.codecraft.util.maths.{Rng, Vector2}


private[core] class Mothership extends DroneController {
  var t = 0
  var collectors = 0

  val collectorDroneSpec = new DroneSpec(4, storageModules = 2)
  val fastCollectorDroneSpec = new DroneSpec(4, storageModules = 1, engines = 1)
  val attackDroneSpec = new DroneSpec(4, missileBatteries = 2)

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildDrone(new DroneSpec(3, storageModules = 1), new ScoutingDroneController(this))
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      if (collectors < 2) {
        buildDrone(if (Rng.bernoulli(0.9f)) collectorDroneSpec else fastCollectorDroneSpec, new ScoutingDroneController(this))
        collectors += 1
      } else {
        buildDrone(attackDroneSpec, new AttackDroneController())
      }
    } else {
      for (mineralCrystal <- storedMinerals) {
        if (availableFactories >= mineralCrystal.size) {
          processMineral(mineralCrystal)
        }
      }
    }

    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = enemies.minBy(x => (x.position - position).magnitudeSquared)
      if (isInMissileRange(enemy)) {
        shootMissiles(enemy)
      }
    }
  }

  def enemies: Set[DroneHandle] =
    dronesInSight.filter(_.player != player)

  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()
  override def onArrivesAtPosition(): Unit = ()
  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
  override def onDeath(): Unit = ()
}

private[core] class ScoutingDroneController(val mothership: Mothership) extends DroneController {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystalHandle] = None


  // abstract methods for event handling
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onDeath(): Unit = mothership.collectors -= 1

  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = {
    if (nextCrystal.isEmpty && mineralCrystal.size <= availableStorage) {
      moveTo(mineralCrystal.position)
      nextCrystal = Some(mineralCrystal)
    }
  }

  override def onTick(): Unit = {
    if (availableStorage == 0 && !hasReturned) {
      moveTo(mothership)
    } else if ((hasReturned && availableStorage > 0) || Rng.bernoulli(0.005) && nextCrystal == None) {
      hasReturned = false
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  override def onArrivesAtPosition(): Unit = {
    if (availableStorage == 0) {
      depositMinerals(mothership)
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

  override def onArrivesAtDrone(drone: DroneHandle): Unit = {
    depositMinerals(drone)
    hasReturned = true
  }

  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
}

private[core] class AttackDroneController extends DroneController {
  // abstract methods for event handling
  override def onSpawn(): Unit = ()

  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()

  override def onTick(): Unit = {
    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = enemies.minBy(x => (x.position - position).magnitudeSquared)
      if (isInMissileRange(enemy)) {
        shootMissiles(enemy)
      }
      moveInDirection(enemy.position - position)
    } else if (Rng.bernoulli(0.01)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }

  def enemies: Set[DroneHandle] =
    dronesInSight.filter(_.player != player)

  override def onArrivesAtPosition(): Unit = ()

  override def onDroneEntersVision(drone: DroneHandle): Unit = ()
  override def onDeath(): Unit = ()
}

