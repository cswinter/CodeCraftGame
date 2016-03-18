package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.{Drone, DroneController, MineralCrystal}


private[codecraft] abstract class AugmentedController[TCommand, TContext <: SharedContext[TCommand]](
  val context: TContext
) extends DroneController {
  var searchToken: Option[SearchToken] = None
  context.droneCount.increment(getClass)

  @inline final def enemies: Set[Drone] = enemiesInSight
  def armedEnemies: Set[Drone] =
    enemiesInSight.filter(_.spec.missileBatteries > 0)

  def optimalTarget: Option[Drone] = {
    val inRange = enemies.filter(isInMissileRange)
    if (inRange.isEmpty) None
    else Some(inRange.maxBy(x => x.spec.missileBatteries.toFloat / x.hitpoints))
  }

  def closestEnemy: Drone = enemiesInSight.minBy(x => (x.position - position).lengthSquared)

  def closestEnemyAndDist2: (Drone, Double) =
    enemies.map(x => (x, (x.position - position).lengthSquared)).minBy(_._2)

  def handleWeapons(): Unit =
    if (missileCooldown <= 0)
      for (target <- optimalTarget)
        fireMissilesAt(target)

  def calculateStrength(drones: Iterable[Drone]): Int = {
    val (health, attack) = drones.foldLeft(0, 0){
      case ((h, a), d) => (h + d.hitpoints, a + d.spec.missileBatteries)
    }
    health * attack
  }

  def canWin: Boolean = {
    val (enemyStrength, alliedStrength) = calculateStrength
    enemyStrength <= alliedStrength
  }

  def enemyStronger: Boolean = {
    val (enemyStrength, alliedStrength) = calculateStrength
    alliedStrength <= enemyStrength
  }

  def calculateStrength: (Int, Int) = {
    val enemyStrength = calculateStrength(dronesInSight.filter(_.isEnemy))
    val alliedStrength = calculateStrength(dronesInSight.filterNot(_.isEnemy) + this)
    (enemyStrength, alliedStrength)
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

  def requestSearchToken(): Option[SearchToken] = {
    context.searchCoordinator.getSearchToken(position)
  }

  override def onDeath(): Unit = {
    for (st <- searchToken) context.searchCoordinator.returnSearchToken(st)
    context.droneCount.decrement(getClass)
  }

  override def onConstructionCancelled(): Unit = {
    context.droneCount.decrement(getClass)
  }

  override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit =
    context.harvestCoordinator.registerMineral(mineralCrystal)

  override def onDroneEntersVision(drone: Drone): Unit =
    if (drone.isEnemy && drone.spec.constructors > 0) {
      context.battleCoordinator.foundCapitalShip(drone)
    }

  override def metaController = Some(context)
}

