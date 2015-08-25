package cwinter.codecraft.core.ai.basicplus

import cwinter.codecraft.core.api.{MineralCrystal, Drone, DroneController}

abstract class BaseController(val name: Symbol) extends DroneController {
  val mothership: Mothership
  var searchToken: Option[SearchToken] = None

  if (name != 'Mothership)
    mothership.DroneCount.increment(this.name)

  def enemies: Set[Drone] =
    dronesInSight.filter(_.player != player)

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
    mothership.getSearchToken(position)
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

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit =
    mothership.registerMineral(mineralCrystal)

  override def onArrivesAtPosition(): Unit = ()
  override def onDroneEntersVision(drone: Drone): Unit = {
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
