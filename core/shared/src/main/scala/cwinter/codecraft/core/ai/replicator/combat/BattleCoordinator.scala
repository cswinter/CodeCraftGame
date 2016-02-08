package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.Soldier
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




