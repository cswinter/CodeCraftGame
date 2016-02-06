package cwinter.codecraft.testai.replicator

import cwinter.codecraft.core.api.{Drone, DroneController, MineralCrystal}


abstract class ReplicatorBase(
  val name: Symbol,
  val context: ReplicatorContext
) extends DroneController {
  var searchToken: Option[SearchToken] = None
  context.droneCount.increment(name)

  def enemies: Set[Drone] =
    dronesInSight.filter(_.playerID != playerID)

  def closestEnemy: Drone = enemies.minBy(x => (x.position - position).lengthSquared)

  def handleWeapons(): Unit = {
    if (weaponsCooldown <= 0 && enemies.nonEmpty) {
      val enemy = closestEnemy
      if (isInMissileRange(enemy)) {
        fireMissilesAt(enemy)
      }
    }
  }

  def calculateStrength(drones: Iterable[Drone]): Int = {
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
    context.searchCoordinator.getSearchToken(position)
  }

  def scout(): Unit = {
    if (searchToken.isEmpty) searchToken = requestSearchToken()
    for (t <- searchToken) {
      if ((position - t.pos).lengthSquared < 1) {
        searchToken = None
      } else {
        moveTo(t.pos)
      }
    }
  }

  override def onDeath(): Unit = {
    for (st <- searchToken) context.searchCoordinator.returnSearchToken(st)
    context.droneCount.decrement(name)
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit =
    context.harvestCoordinator.registerMineral(mineralCrystal)

  override def onDroneEntersVision(drone: Drone): Unit = {
    if (drone.isEnemy)
    if (drone.isEnemy && drone.spec.size > 6) {
      context.battleCoordinator.foundCapitalShip(drone)
    }
  }
}
