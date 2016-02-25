package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.{TargetAcquisition, ReplicatorController}
import cwinter.codecraft.core.ai.shared.BattleCoordinator
import cwinter.codecraft.core.api.{DroneSpec, Drone}
import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.util.maths.{Vector2, ColorRGBA}


class ReplicatorBattleCoordinator extends BattleCoordinator[ReplicatorCommand] {
  private[this] var assisting = Map.empty[ReplicatorController, Assist]
  private[this] var guarding = Map.empty[ReplicatorController, Guard]
  private[this] var enemyForces = Set.empty[Drone]
  private[this] var targetRegistry = Map.empty[Drone, Set[TargetAcquisition]]
  private[this] var _enemyStrength = 0.0
  private[this] var _enemyClusters = Map.empty[Drone, EnemyCluster]
  def enemyStrength = _enemyStrength
  final val SoldierStrength = 2
  addMission(ScoutingMission)


  override def update(): Unit = {
    super.update()

    analyzeEnemyForces()

    assisting = assisting.filter(!_._2.hasExpired)
    guarding = guarding.filter(!_._2.hasExpired)
  }

  def analyzeEnemyForces(): Unit = {
    purgeDeadEnemies()
    _enemyStrength = computeNormalizedStrength(enemyForces)
    determineEnemyClusters()
    //for (c <- _enemyClusters.values.toSet[EnemyCluster]) c.show()
  }

  def purgeDeadEnemies(): Unit = enemyForces = enemyForces.filter(!_.isDead)

  def computeNormalizedStrength(drones: Iterable[Drone]): Double =
    drones.foldLeft(0.0){
      (acc, d) =>
        val hitpoints = if (d.isVisible) d.hitpoints else d.spec.maxHitpoints
        acc + math.sqrt(hitpoints * d.spec.missileBatteries)
    } / SoldierStrength

  def determineEnemyClusters(): Unit = {
    val maxDist2 = 400 * 400
    var clusters = Map.empty[Drone, EnemyCluster]
    var visited = Set.empty[Drone]
    for (drone <- enemyForces) {
      val closeby = visited.find(d => (d.lastKnownPosition - drone.lastKnownPosition).lengthSquared < maxDist2)
      closeby match {
        case None =>
          val newCluster = new EnemyCluster
          newCluster.add(drone)
          clusters += drone -> newCluster
        case Some(d) =>
          val cluster = clusters(d)
          cluster.add(drone)
          clusters += drone -> cluster
      }
      visited += drone
    }
    _enemyClusters = clusters
  }

  def requestAssistance(drone: ReplicatorController): Unit = {
    if (assisting.contains(drone)) assisting(drone).refresh()
    else {
      val (priority, radius) =
        if (drone.spec.constructors > 0) (15, 950) else (5, 750)
      val assistMission = new Assist(drone, priority, math.ceil(drone.normalizedEnemyCount / 2).toInt - 1, radius)
      assisting += drone -> assistMission
      addMission(assistMission)
    }
  }

  def requestGuards(drone: ReplicatorController, amount: Int): Unit = {
    if (guarding.contains(drone)) guarding(drone).refresh(amount)
    else {
      val guardMission = new Guard(drone, amount)
      guarding += drone -> guardMission
      addMission(guardMission)
    }
  }

  def isCovered(enemy: Drone): Boolean =
    _enemyClusters.contains(enemy) && _enemyClusters(enemy).isCovered

  def notTargeting(enemy: Drone, executor: TargetAcquisition): Unit = {
    targetRegistry = targetRegistry.updated(enemy, targetRegistry(enemy) - executor)
  }

  def targeting(enemy: Drone, executor: TargetAcquisition): Unit = {
    targetRegistry = targetRegistry.updated(enemy, targetRegistry(enemy) + executor)
  }

  override def foundCapitalShip(drone: Drone): Unit = {
    if (!enemyCapitalShips.contains(drone)) {
      val newMission = new AssaultCapitalShip(drone)
      addMission(newMission)
    }
    super.foundCapitalShip(drone)
  }

  def foundArmedEnemy(drone: Drone): Unit = {
    if (!enemyForces.contains(drone)) {
      enemyForces += drone
      targetRegistry += drone -> Set.empty[TargetAcquisition]
      if (drone.spec.maximumSpeed <= DroneSpec(missileBatteries = 1).maximumSpeed) {
        val followMission = new KeepEyeOnEnemy(drone)
        addMission(followMission)
      }
    }
  }

  class EnemyCluster {
    private[this] var _drones = Set.empty[Drone]
    def add(drone: Drone): Unit = _drones += drone
    def show(): Unit = {
      val midpoint = _drones.foldLeft(Vector2.Null)(_ + _.lastKnownPosition) / _drones.size
      Debug.drawText(s"Size ${_drones.size} cluster", midpoint.x, midpoint.y, ColorRGBA(1, 0, 1, 1))
    }

    def isCovered: Boolean = {
      val totalStrength = computeNormalizedStrength(_drones)
      val totalCover = _drones.flatMap(targetRegistry).foldLeft(0.0)(_ + _.normalizedStrength)
      totalStrength <= totalCover
    }
  }
}

