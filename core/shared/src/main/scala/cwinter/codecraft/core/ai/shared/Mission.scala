package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.{Drone, DroneControllerBase}
import cwinter.codecraft.util.maths.Vector2


private[codecraft] trait Mission[TCommand] {
  type Executor = DroneControllerBase with MissionExecutor[TCommand]
  private[this] var _assigned = Set.empty[Executor]
  def assigned: Set[Executor] = _assigned
  private var _isDeactivated: Boolean = false
  def isDeactivated: Boolean = _isDeactivated

  def minRequired: Int
  def maxRequired: Int
  def missionInstructions: TCommand
  def priority: Int
  def hasExpired: Boolean
  def locationPreference: Option[Vector2]

  private[this] def maxString = if (maxRequired == Int.MaxValue) "Inf" else maxRequired
  def assign(hunter: Executor): Unit = {
    _assigned += hunter
    // println(s"$missionInstructions+ (${_assigned.size}/$maxString)")
  }

  def relieve(hunter: Executor): Unit = {
    _assigned -= hunter
    // println(s"$missionInstructions- (${_assigned.size}/$maxString)")
  }

  def disband(): Unit = {
    for (a <- _assigned) a.abortMission()
    // println(s"<$missionInstructions")
  }

  def findSuitableRecruits(candidates: Set[Executor]): Set[Executor] = {
    if (_isDeactivated) Set.empty
    else {
      val eligible =
        if (maxRequired == _assigned.size) Set.empty[Executor]
        else candidates.
          filter(d => d.missionPriority < priority && candidateFilter(d))

      if (eligible.size + _assigned.size < minRequired) Set.empty
      else {
        locationPreference match {
          case None => eligible.take(maxRequired - _assigned.size)
          case Some(pos) =>
            eligible.toSeq.sortBy(d => (d.position - pos).lengthSquared).take(maxRequired - _assigned.size).toSet
        }
      }
    }
  }

  def reduceAssignedToMax(): Unit = {
    while (maxRequired < nAssigned && nAssigned > 0) {
      _assigned.head.abortMission()
    }
  }

  def update(): Unit = ()
  def candidateFilter(drone: Drone): Boolean = true

  def deactivate(): Unit = {
    disband()
    _isDeactivated = true
  }

  def reactivate(): Unit = _isDeactivated = false

  def nAssigned: Int = _assigned.size
}

private[codecraft] trait MissionExecutor[TCommand] {
  self: DroneControllerBase =>

  private[this] var _mission: Option[Mission[TCommand]] = None
  def mission: Option[Mission[TCommand]] = _mission

  def startMission(mission: Mission[TCommand]): Unit = {
    mission.assign(this)
    _mission = Some(mission)
  }

  def abortMission(): Unit = {
    for (m <- _mission) m.relieve(this)
    _mission = None
  }

  def missionPriority: Int = _mission.map(_.priority).getOrElse(-1)
}

