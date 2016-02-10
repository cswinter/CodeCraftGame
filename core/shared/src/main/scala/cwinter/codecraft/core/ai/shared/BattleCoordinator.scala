package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.{DroneControllerBase, Drone}


trait BattleCoordinator[TCommand] {
  type Executor = DroneControllerBase with MissionExecutor[TCommand]
  private[this] var _enemyCapitalShips = Set.empty[Drone]
  private[this] var _missions = List.empty[Mission[TCommand]]
  private[this] var executors = Set.empty[Executor]


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
      val recruits = m.findSuitableRecruits(executors)
      for (r <- recruits)
        r.abortMission()
      for (r <- recruits)
        r.startMission(m)
    }
  }

  def foundCapitalShip(drone: Drone): Unit = {
    if (!_enemyCapitalShips.contains(drone)) {
      _enemyCapitalShips += drone
    }
  }

  def online(hunter: Executor): Unit =
    executors += hunter

  def offline(hunter: Executor): Unit = {
    executors -= hunter
  }

  def addMission(mission: Mission[TCommand]): Unit = _missions ::= mission
}
