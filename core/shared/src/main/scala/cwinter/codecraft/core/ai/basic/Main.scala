package cwinter.codecraft.core.ai.basic

import cwinter.codecraft.core.api.{Drone, DroneController, DroneSpec, MineralCrystal}
import cwinter.codecraft.util.maths.{GlobalRNG, RNG, Vector2}

/** Default mothership */
private[core] class Mothership extends DroneController {
  var t = 0
  var collectors = 0

  val collectorDroneSpec = DroneSpec(storageModules = 2)
  val fastCollectorDroneSpec = DroneSpec(storageModules = 1, engines = 1)
  val attackDroneSpec = DroneSpec(missileBatteries = 2)

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildDrone(new ScoutingDroneController(this), DroneSpec(storageModules = 1))
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      if (collectors < 2) {
        buildDrone(new ScoutingDroneController(this), if (GlobalRNG.bernoulli(0.9f)) collectorDroneSpec else fastCollectorDroneSpec)
        collectors += 1
      } else {
        buildDrone(new AttackDroneController(), attackDroneSpec)
      }
    }

    if (missileCooldown <= 0 && enemies.nonEmpty) {
      val enemy = enemies.minBy(x => (x.position - position).lengthSquared)
      if (isInMissileRange(enemy)) {
        fireMissilesAt(enemy)
      }
    }
  }

  def enemies: Set[Drone] =
    dronesInSight.filter(_.playerID != playerID)

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onArrivesAtPosition(): Unit = ()
  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

private[core] class ScoutingDroneController(val mothership: Mothership) extends DroneController {
  private var nextMineral = Option.empty[MineralCrystal]

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = {
    if (nextMineral.isEmpty && availableStorage > 0) {
      nextMineral = Some(mineralCrystal)
      moveTo(mineralCrystal)
    }
  }

  override def onTick(): Unit = {
    if (nextMineral.exists(_.harvested)) nextMineral = None
    if (!isMoving && !isHarvesting) {
      if (availableStorage == 0) { moveTo(mothership); nextMineral = None }
      else moveTo(position + GlobalRNG.vector2(500))
    }
  }

  override def onArrivesAtMineral(mineral: MineralCrystal): Unit = harvest(mineral)

  override def onArrivesAtDrone(drone: Drone): Unit = giveResourcesTo(drone)

  override def onDeath(): Unit = mothership.collectors -= 1
}

private[core] class AttackDroneController extends DroneController {
  // abstract methods for event handling
  override def onSpawn(): Unit = ()

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

  override def onTick(): Unit = {
    if (missileCooldown == 0 && enemies.nonEmpty) {
      val enemy = enemies.minBy(x => (x.position - position).lengthSquared)
      if (isInMissileRange(enemy)) {
        fireMissilesAt(enemy)
      }
      moveInDirection(enemy.position - position)
    } else if (GlobalRNG.bernoulli(0.01)) {
      moveInDirection(Vector2(GlobalRNG.double(0, 100)))
    }
  }

  def enemies: Set[Drone] =
    dronesInSight.filter(_.playerID != playerID)

  override def onArrivesAtPosition(): Unit = ()

  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

