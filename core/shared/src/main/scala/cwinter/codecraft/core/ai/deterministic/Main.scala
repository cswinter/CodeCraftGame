package cwinter.codecraft.core.ai.deterministic

import cwinter.codecraft.core.api.{Drone, DroneController, DroneSpec, MineralCrystal}
import cwinter.codecraft.util.maths.{RNG, Vector2}


private[codecraft] class DeterministicMothership(rngSeed: Int) extends DroneController {
  val rng = new RNG(rngSeed)
  var t = 0
  var collectors = 0

  val collectorDroneSpec = new DroneSpec(storageModules = 1)
  val fastCollectorDroneSpec = new DroneSpec(storageModules = 1, engines = 1)
  val attackDroneSpec1 = new DroneSpec(missileBatteries = 1, shieldGenerators = 1)
  val attackDroneSpec2 = new DroneSpec(missileBatteries = 2)

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildDrone(new DeterministicScout(this), new DroneSpec(storageModules = 1))
  }

  override def onTick(): Unit = {
    if (!isConstructing) {
      if (collectors < 2) {
        buildDrone(new DeterministicScout(this), if (rng.bernoulli(0.7f)) collectorDroneSpec else fastCollectorDroneSpec)
        collectors += 1
      } else {
        val spec = if (rng.bernoulli(0.5f)) attackDroneSpec1 else attackDroneSpec2
        buildDrone(new DeterministicSoldier(rng), spec)
      }
    }

    if (missileCooldown <= 0 && enemiesInSight.nonEmpty) {
      val enemy = enemiesInSight.min(DroneDistanceOrdering(position))
      if (isInMissileRange(enemy)) {
        fireMissilesAt(enemy)
      }
    }
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
  override def onArrivesAtPosition(): Unit = ()
  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

private[core] class DeterministicScout(val mothership: DeterministicMothership) extends DroneController {
  private var nextMineral = Option.empty[MineralCrystal]
  private var fleeing = false

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = {
    if (nextMineral.forall(hasPriority(mineralCrystal, _)) && availableStorage > 0) {
      nextMineral = Some(mineralCrystal)
      moveTo(mineralCrystal)
    }
  }

  override def onTick(): Unit = {
    if (nextMineral.exists(_.harvested)) nextMineral = None
    if (enemiesInSight.nonEmpty) {
      val closest = enemiesInSight.min(DroneDistanceOrdering(position))
      moveInDirection(position - closest.position)
      fleeing = true
    } else if ((!isMoving || fleeing) && !isHarvesting) {
      fleeing = false
      if (availableStorage == 0) { moveTo(mothership); nextMineral = None }
      else {
        nextMineral match {
          case Some(mineral) => moveTo(mineral)
          case None =>
            moveTo(position + mothership.rng.vector2(500))
        }
      }
    }
  }

  override def onArrivesAtMineral(mineral: MineralCrystal): Unit = harvest(mineral)

  override def onArrivesAtDrone(drone: Drone): Unit = giveResourcesTo(drone)

  override def onDeath(): Unit = mothership.collectors -= 1

  private def hasPriority(m1: MineralCrystal, m2: MineralCrystal): Boolean = {
    val dist1 = (m1.position - position).lengthSquared
    val dist2 = (m2.position - position).lengthSquared
    dist1 < dist2 || (dist1 == dist2 && m1.mineralCrystal.id < m2.mineralCrystal.id)
  }
}

private[core] class DeterministicSoldier(rng: RNG) extends DroneController {
  // abstract methods for event handling
  override def onSpawn(): Unit = ()

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

  override def onTick(): Unit = {
    if (enemies.nonEmpty) {
      val enemy = enemies.min(DroneDistanceOrdering(position))
      if (missileCooldown == 0 && isInMissileRange(enemy)) fireMissilesAt(enemy)
      moveTo(enemy.position)
    } else if (!isMoving) {
      moveTo(Vector2(rng.double(worldSize.xMin, worldSize.xMax), rng.double(worldSize.yMin, worldSize.yMax)))
    }
  }

  def enemies: Set[Drone] =
    dronesInSight.filter(_.playerID != playerID)

  override def onArrivesAtPosition(): Unit = ()

  override def onDroneEntersVision(drone: Drone): Unit = ()
  override def onDeath(): Unit = ()
}

case class DroneDistanceOrdering(origin: Vector2) extends Ordering[Drone] {
  override def compare(drone1: Drone, drone2: Drone): Int = {
    val dist1 = (drone1.position - origin).lengthSquared
    val dist2 = (drone2.position - origin).lengthSquared
    if (dist1 < dist2) -1
    else if (dist2 < dist1) 1
    else drone1.drone.priority - drone2.drone.priority
  }
}
