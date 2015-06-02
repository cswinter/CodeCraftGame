package cwinter.codinggame.testai

import cwinter.codinggame.core.api._
import cwinter.codinggame.core.replay.Replayer
import cwinter.codinggame.util.maths.{Vector2, Rng}
import cwinter.codinggame.worldstate.BluePlayer

import scala.reflect.ClassTag

object Main {
  def main(args: Array[String]): Unit = {
    // TheGameMaster.runLevel1(new Mothership)
    // TheGameMaster.startGame(new Mothership, new Mothership)
     TheGameMaster.runReplay(new Replayer(scala.io.Source.fromFile("/home/clemens/replay.txt").getLines()))
  }
}

abstract class BaseController(val name: Symbol) extends DroneController {
  val mothership: Mothership
  var searchToken: Option[SearchToken] = None

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

  def calculateStrength(drones: Iterable[DroneHandle]): Int = {
    val (health, attack) = drones.foldLeft(0, 0){
      case ((h, a), d) => (h + d.hitpoints, a + d.spec.missileBatteries)
    }
    health * attack
  }

  def canWin: Boolean = {
    val enemyStrength = calculateStrength(dronesInSight.filter(_.isEnemy))
    val alliedStrength = calculateStrength((dronesInSight + this).filterNot(_.isEnemy))
    enemyStrength <= alliedStrength
  }

  def requestSearchToken(): Option[SearchToken] = {
    mothership.getSearchToken(position)
  }
  def scout(): Unit = {
    if (searchToken == None) searchToken = requestSearchToken()
    for (t <- searchToken) {
      if ((position - t.pos).magnitudeSquared < 1) {
        searchToken = None
      } else {
        moveToPosition(t.pos)
      }
    }
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
  override def onSpawn(): Unit = ()
}

class Mothership extends BaseController('Mothership) {
  val mothership = this
  var defenders = List.empty[DroneHandle]
  var defenderCooldown: Int = 150

  var t = 0
  var minerals = Set.empty[MineralCrystalHandle]
  var claimedMinerals = Set.empty[MineralCrystalHandle]
  private[this] var _lastCapitalShipSighting: Option[Vector2] = None
  def lastCapitalShipSighting: Option[Vector2] = _lastCapitalShipSighting

  val scoutSpec = DroneSpec(3, storageModules = 1)
  val collectorSpec = DroneSpec(4, storageModules = 2)
  val hunterSpec = DroneSpec(4, missileBatteries = 1, engines = 1)
  val destroyerSpec = DroneSpec(5, missileBatteries = 3, shieldGenerators = 1)
  var searchTokens: Set[SearchToken] = null

  // abstract methods for event handling
  override def onSpawn(): Unit = {
    buildDrone(scoutSpec, new ScoutingDroneController(this))
    searchTokens = genSearchTokens
  }

  override def onTick(): Unit = {
    t += 1
    defenderCooldown -= 1
    if (!isConstructing) {
      if (DroneCount('Harvester) < 1 ||
        (DroneCount('Hunter) > 0 && DroneCount('Harvester) < 3) ||
        (DroneCount('Destroyer) > 0 && DroneCount('Hunter) > 0 && DroneCount('Harvester) < 4)) {
        buildDrone(collectorSpec, new ScoutingDroneController(this))
      } else if (2 * DroneCount('Hunter) / math.max(DroneCount('Destroyer), 1) < 1) {
        buildDrone(hunterSpec, new Hunter(this))
      } else {
        buildDrone(destroyerSpec, new Destroyer(this))
      }
    }

    handleWeapons()
  }


  def needsDefender: Boolean = {
    if (hitpoints == 0) return false
    val strength = calculateStrength(defenders)
    val enemyStrength = calculateStrength(enemies)
    strength < enemyStrength
  }

  def registerDefender(droneHandle: DroneHandle): Unit = {
    defenders ::= droneHandle
    defenderCooldown = 150
  }

  def allowsDefenderRelease: Boolean =
    defenderCooldown <= 0 && !needsDefender

  def unregisterDefender(droneHandle: DroneHandle): Unit = {
    defenders = defenders.filter(_ != droneHandle)
  }

  def foundCapitalShip(drone: DroneHandle): Unit = {
    _lastCapitalShipSighting = Some(drone.position)
  }

  def findClosestMineral(maxSize: Int, position: Vector2): Option[MineralCrystalHandle] = {
    minerals = minerals.filter(!_.harvested)
    val filtered = minerals.filter(m => m.size <= maxSize && !claimedMinerals.contains(m))
    val result =
      if (filtered.isEmpty) None
      else Some(filtered.minBy(m => (m.position - position).magnitudeSquared))
    for (m <- result) {
      claimedMinerals += m
    }
    result
  }

  def registerMineral(mineralCrystal: MineralCrystalHandle): Unit = {
    minerals += mineralCrystal
  }

  def abortHarvestingMission(mineralCrystal: MineralCrystalHandle): Unit = {
    claimedMinerals -= mineralCrystal
  }

  def getSearchToken(pos: Vector2): Option[SearchToken] = {
    if (searchTokens.isEmpty) {
      None
    } else {
      val closest = searchTokens.minBy(t => (t.pos - pos).magnitudeSquared)
      searchTokens -= closest
      Some(closest)
    }
  }

  private def genSearchTokens: Set[SearchToken] = {
    val width = math.ceil(worldSize.width / DroneSpec.SightRadius).toInt
    val height = math.ceil(worldSize.height / DroneSpec.SightRadius).toInt
    val xOffset = (worldSize.width / DroneSpec.SightRadius / 2).toInt
    val yOffset = (worldSize.height / DroneSpec.SightRadius / 2).toInt
    val tokens = Seq.tabulate(width, height){
      (x, y) => SearchToken(x - xOffset, y - yOffset)
    }
    for (ts <- tokens; t <- ts) yield t
  }.toSet


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

case class SearchToken(x: Int, y: Int) {
  val pos: Vector2 = Vector2((x + 0.5) * DroneSpec.SightRadius, (y + 0.5) * DroneSpec.SightRadius)
}

class ScoutingDroneController(val mothership: Mothership) extends BaseController('Harvester) {
  var hasReturned = false
  var nextCrystal: Option[MineralCrystalHandle] = None


  override def onTick(): Unit = {

    if (nextCrystal.exists(_.harvested)) nextCrystal = None
    if (nextCrystal == None) nextCrystal = mothership.findClosestMineral(availableStorage, position)

    if (enemies.nonEmpty && closestEnemy.spec.missileBatteries > 0) {
      moveInDirection(position - closestEnemy.position)
    } else {

      if (availableStorage == 0 && !hasReturned) {
        moveToDrone(mothership)
        nextCrystal = None
      } else if (hasReturned && availableStorage > 0 || nextCrystal == None) {
        hasReturned = false
        scout()
      } else {
        for (
          c <- nextCrystal
          if !(c.position ~ position)
        ) moveToPosition(c.position)
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


  override def onDeath(): Unit = {
    for (m <- nextCrystal)
      mothership.abortHarvestingMission(m)
  }
}

class Hunter(val mothership: Mothership) extends BaseController('Hunter) {
  override def onTick(): Unit = {
    handleWeapons()

    if (enemies.nonEmpty) {
      val closest = closestEnemy
      if (closest.spec.missileBatteries > 0) {
        if (!canWin) {
          moveInDirection(position - closest.position)
        }
      } else {
        moveInDirection(closest.position - position)
      }
    }

    if (Rng.bernoulli(0.005)) {
      moveToPosition(0.9 * Rng.vector2(worldSize))
    }
  }
}

class Destroyer(val mothership: Mothership) extends BaseController('Destroyer) {
  var attack = false
  var defend = false

  override def onSpawn(): Unit = {
    moveInDirection(Vector2(Rng.double(0, 100)))
  }

  override def onTick(): Unit = {
    handleWeapons()

    if (!defend && mothership.needsDefender) {
      mothership.registerDefender(this)
      defend = true
    }
    if (defend && mothership.allowsDefenderRelease) {
      mothership.unregisterDefender(this)
      defend = false
    }
    if (mothership.t % 600 == 0) attack = true

    if (enemies.nonEmpty) {
      val pClosest = closestEnemy.position
      if (canWin || defend) {
        moveInDirection(pClosest - position)
      } else {
        moveInDirection(position - pClosest)
        attack = false
      }
    } else if (defend) {
      if ((position - mothership.position).magnitudeSquared > 350 * 350) {
        moveToPosition(Rng.double(250, 350) * Rng.vector2() + mothership.position)
      }
    } else if (attack && mothership.lastCapitalShipSighting != None) {
      for (p <- mothership.lastCapitalShipSighting)
        moveToPosition(p)
    } else if (Rng.bernoulli(0.005)) {
      moveToPosition(Rng.double(600, 900) * Rng.vector2() + mothership.position)
    }
  }

  override def onDeath(): Unit = {
    if (defend) mothership.unregisterDefender(this)
  }
}