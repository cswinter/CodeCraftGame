package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Vector2


class BattleCoordinator {
  private[this] var _enemyCapitalShips = Set.empty[Drone]
  private[this] var warriors = Set.empty[Hunter]
  private[this] var _missions = List[Mission](ScoutingMission)

  def update(): Unit = {
    _missions.foreach(_.update())
    purgeExpiredMissions()
    reallocateWarriors()
  }

  def purgeExpiredMissions(): Unit = {
    val (expired, active) = _missions.partition(_.hasExpired)
    for (m <- expired) m.disband()
    _missions = active
  }

  def reallocateWarriors(): Unit = {
    for (m <- _missions) {
      val recruits = m.findSuitableRecruits(warriors)
      for (r <- recruits)
        r.abortMission()
      for (r <- recruits)
        r.startMission(m)
    }
  }

  def foundCapitalShip(drone: Drone): Unit = {
    if (!_enemyCapitalShips.contains(drone)) {
      _enemyCapitalShips += drone
      val newMission = new AssaultCapitalShip(drone)
      _missions ::= newMission
      // println(s">${newMission.missionInstructions}")
    }
  }

  def online(hunter: Hunter): Unit =
    warriors += hunter

  def offline(hunter: Hunter): Unit = {
    warriors -= hunter
  }
}


trait Mission {
  private[this] var assigned = Set.empty[Hunter]

  def minRequired: Int
  def maxRequired: Int
  def missionInstructions: MissionInstructions
  def priority: Int
  def hasExpired: Boolean

  private[this] def maxString = if (maxRequired == Int.MaxValue) "Inf" else maxRequired
  def assign(hunter: Hunter): Unit = {
    assigned += hunter
    // println(s"$missionInstructions+ (${assigned.size}/$maxString)")
  }

  def relieve(hunter: Hunter): Unit = {
    assigned -= hunter
    // println(s"$missionInstructions- (${assigned.size}/$maxString)")
  }

  def disband(): Unit = {
    for (a <- assigned) a.abortMission()
    // println(s"<$missionInstructions")
  }

  def findSuitableRecruits(candidates: Set[Hunter]): Set[Hunter] = {
    val eligible =
      if (maxRequired == assigned.size) Set.empty[Hunter]
      else candidates.filter(_.missionPriority < priority).take(maxRequired - assigned.size)

    if (eligible.size + assigned.size >= minRequired) eligible
    else Set.empty
  }

  def update(): Unit = ()

  def nAssigned: Int = assigned.size
}

object ScoutingMission extends Mission {
  val minRequired = 1
  val maxRequired = Int.MaxValue
  val missionInstructions = Scout
  val priority = 1
  val hasExpired = false
}

class AssaultCapitalShip(enemy: Drone) extends Mission {
  val minRequired = (enemy.spec.missileBatteries + 1) * (enemy.spec.shieldGenerators + 1)
  val maxRequired = minRequired * 2
  val priority = 10

  private var searchRadius = 0.0

  def missionInstructions: MissionInstructions =
    if (enemy.isVisible || searchRadius == 0) Attack(enemy, this)
    else Search(enemy.lastKnownPosition, searchRadius)

  def notFound(): Unit = {
    searchRadius = 500
  }

  override def update(): Unit =
    if (!enemy.isVisible && nAssigned > 0 && searchRadius > 0) searchRadius += 1
    else if (enemy.isVisible) searchRadius = 0

  def hasExpired = enemy.isDead
}


sealed trait MissionInstructions
case object Scout extends MissionInstructions
case class Attack(enemy: Drone, feedback: AssaultCapitalShip) extends MissionInstructions
case class Search(position: Vector2, radius: Double) extends MissionInstructions


