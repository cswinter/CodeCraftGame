package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Vector2


class BattleCoordinator {
  private[this] var _enemyCapitalShips = Set.empty[Drone]
  private[this] var warriors = Set.empty[Soldier]
  private[this] var _missions = List[Mission](ScoutingMission)
  private[this] var assisting = Map.empty[Drone, Assist]

  def update(): Unit = {
    _missions.foreach(_.update())
    purgeExpiredMissions()
    reallocateWarriors()
  }

  def purgeExpiredMissions(): Unit = {
    val (expired, active) = _missions.partition(_.hasExpired)
    for (m <- expired) m.disband()
    _missions = active
    assisting = assisting.filter(!_._2.hasExpired)
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

  def requestAssistance(drone: Drone): Unit = {
    if (assisting.contains(drone)) assisting(drone).refresh()
    else {
      val (priority, radius) =
        if (drone.spec.constructors > 0) (15, 950) else (5, 750)
      val assistMission = new Assist(drone, priority, radius)
      assisting += drone -> assistMission
      _missions ::= assistMission
    }
  }

  def online(hunter: Soldier): Unit =
    warriors += hunter

  def offline(hunter: Soldier): Unit = {
    warriors -= hunter
  }
}


trait Mission {
  private[this] var assigned = Set.empty[Soldier]

  def minRequired: Int
  def maxRequired: Int
  def missionInstructions: MissionInstructions
  def priority: Int
  def hasExpired: Boolean

  private[this] def maxString = if (maxRequired == Int.MaxValue) "Inf" else maxRequired
  def assign(hunter: Soldier): Unit = {
    assigned += hunter
    // println(s"$missionInstructions+ (${assigned.size}/$maxString)")
  }

  def relieve(hunter: Soldier): Unit = {
    assigned -= hunter
    // println(s"$missionInstructions- (${assigned.size}/$maxString)")
  }

  def disband(): Unit = {
    for (a <- assigned) a.abortMission()
    // println(s"<$missionInstructions")
  }

  def findSuitableRecruits(candidates: Set[Soldier]): Set[Soldier] = {
    val eligible =
      if (maxRequired == assigned.size) Set.empty[Soldier]
      else candidates.
        filter(d => d.missionPriority < priority && candidateFilter(d)).
        take(maxRequired - assigned.size)

    if (eligible.size + assigned.size >= minRequired) eligible
    else Set.empty
  }

  def update(): Unit = ()
  def candidateFilter(drone: Drone): Boolean = true

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

class Assist(
  val friend: Drone,
  val priority: Int,
  radius: Int
) extends Mission {
  val radius2 = radius * radius
  var timeout = 10

  val minRequired = 1
  val maxRequired = Int.MaxValue

  def missionInstructions = AttackMove(friend.position)
  def  hasExpired = timeout <= 0
  override def update(): Unit = timeout -= 1
  override def candidateFilter(drone: Drone): Boolean =
    (drone.position - friend.position).lengthSquared <= radius2

  def refresh(): Unit = timeout = 10
}


sealed trait MissionInstructions
case object Scout extends MissionInstructions
case class Attack(enemy: Drone, feedback: AssaultCapitalShip) extends MissionInstructions
case class Search(position: Vector2, radius: Double) extends MissionInstructions
case class AttackMove(position: Vector2) extends MissionInstructions

