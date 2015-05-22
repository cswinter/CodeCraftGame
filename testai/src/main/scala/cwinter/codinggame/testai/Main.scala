package cwinter.codinggame.testai

import cwinter.codinggame.core.api._
import cwinter.codinggame.util.maths.{Vector2, Rng}

import scala.reflect.ClassTag

object Main {
  def main(args: Array[String]): Unit = {
    //TheGameMaster.runLevel1(new Mothership)
    TheGameMaster.startGame(new Mothership, new Mothership)
  }
}

abstract class BaseController(val name: Symbol) extends DroneController {
  val mothership: Mothership

  if (name != 'Mothership)
    mothership.DroneCount.increment(this.name)

  def enemies: Set[DroneHandle] =
    dronesInSight.filter(_.player != player)

  def closestEnemy: DroneHandle = enemies.minBy(x => (x.position - position).magnitudeSquared)

  def handleWeapons(): Unit = {
    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = closestEnemy
      if (isInMissileRange(enemy)) {
        shootMissiles(enemy)
      }
    }
  }

  def canWin: Boolean = {
    def calculateStrength(drones: Iterable[DroneHandle]): Int = {
      val (health, attack) = drones.foldLeft(0, 0){
        case ((h, a), d) => (h + d.hitpoints, a + d.spec.missileBatteries)
      }
      health * attack
    }
    val enemyStrength = calculateStrength(dronesInSight.filter(_.isEnemy))
    val alliedStrength = calculateStrength((dronesInSight + this).filterNot(_.isEnemy))
    enemyStrength <= alliedStrength
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit =
    mothership.registerMineral(mineralCrystal)

  override def onArrival(): Unit = ()
  override def onDroneEntersVision(drone: DroneHandle): Unit = {
    if (drone.isEnemy)
    if (drone.isEnemy && drone.spec.size > 6) {
      mothership.foundCapitalShip(drone)
    }
  }
  override def onDeath(): Unit = {
    mothership.DroneCount.decrement(this.name)
  }
}

class Mothership extends BaseController('Mothership) {
  val mothership = this

  var t = 0
  var minerals = Set.empty[MineralCrystalHandle]
  private[this] var _lastCapitalShipSighting: Option[Vector2] = None
  def lastCapitalShipSighting: Option[Vector2] = _lastCapitalShipSighting

  val scoutSpec = DroneSpec(3, storageModules = 1)
  val collectorSpec = DroneSpec(4, storageModules = 2)
  val hunterSpec = DroneSpec(4, missileBatteries = 1, engines = 1)
  val destroyerSpec = DroneSpec(5, missileBatteries = 3, shieldGenerators = 1)


  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildDrone(scoutSpec, new ScoutingDroneController(this))
  }

  override def onTick(): Unit = {
    t += 1
    if (!isConstructing) {
      if (DroneCount('Harvester) < 3) {
        buildDrone(collectorSpec, new ScoutingDroneController(this))
      } else if (2 * DroneCount('Hunter) / math.max(DroneCount('Destroyer), 1) < 1) {
        buildDrone(hunterSpec, new Hunter(this))
      } else {
        buildDrone(destroyerSpec, new Destroyer(this))
      }
    }

    handleWeapons()
  }

  def foundCapitalShip(drone: DroneHandle): Unit = {
    _lastCapitalShipSighting = Some(drone.position)
  }

  def findClosestMineral(maxSize: Int, position: Vector2): Option[MineralCrystalHandle] = {
    minerals = minerals.filter(!_.harvested)
    val filtered = minerals.filter(_.size <= maxSize)
    if (filtered.isEmpty) None
    else Some(filtered.minBy(m => (m.position - position).magnitudeSquared))
  }

  def registerMineral(mineralCrystal: MineralCrystalHandle): Unit = {
    minerals += mineralCrystal
  }


  object DroneCount {
    private[this] var counts = Map.empty[Symbol, Int]

    def apply(name: Symbol): Int = {
      counts.getOrElse(name, 0)
    }

    def increment(name: Symbol): Unit = {
      counts = counts.updated(name, DroneCount(name) + 1)
    }

    def decrement(name: Symbol): Unit = {
      counts = counts.updated(name, DroneCount(name) - 1)
    }
  }
}

class ScoutingDroneController(val mothership: Mothership) extends BaseController('Harvester) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystalHandle] = None


  // abstract methods for event handling
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }


  override def onTick(): Unit = {
    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal == None) nextCrystal = mothership.findClosestMineral(availableStorage, position)

    if (enemies.nonEmpty && closestEnemy.spec.missileBatteries > 0) {
      moveInDirection(position - closestEnemy.position)
    } else {
      for (
        c <- nextCrystal
        if !(c.position ~ position)
      ) moveToPosition(c.position)

      if (availableStorage == 0 && !hasReturned) {
        moveToDrone(mothership)
        nextCrystal = None
      } else if ((hasReturned && availableStorage > 0) || Rng.bernoulli(0.005) && nextCrystal == None) {
        hasReturned = false
        moveInDirection(Vector2(Rng.double(0, 100)))
      }
    }
  }

  override def onArrival(): Unit = {
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
}

class Hunter(val mothership: Mothership) extends BaseController('Hunter) {
  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onTick(): Unit = {
    handleWeapons()

    if (enemies.nonEmpty) {
      val closest = closestEnemy
      if (closest.spec.missileBatteries > 0) {
        moveInDirection(position - closest.position)
      } else {
        moveInDirection(closest.position - position)
      }
    }

    if (Rng.bernoulli(0.01)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }
}

class Destroyer(val mothership: Mothership) extends BaseController('Destroyer) {
  var attack = false


  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onTick(): Unit = {
    handleWeapons()

    if (mothership.t % 600 == 0) attack = true

    if (enemies.nonEmpty) {
      val pClosest = closestEnemy.position
      if (canWin) {
        moveInDirection(pClosest - position)
      } else {
        moveInDirection(position - pClosest)
        attack = false
      }
    } else if (attack) {
      for (p <- mothership.lastCapitalShipSighting)
        moveToPosition(p)
    } else if (Rng.bernoulli(0.01)) {
      moveInDirection(Vector2(Rng.double(0, 100)))
    }
  }
}