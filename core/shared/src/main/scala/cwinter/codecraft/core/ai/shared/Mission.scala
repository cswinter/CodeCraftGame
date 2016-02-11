package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.{Drone, DroneControllerBase}
import cwinter.codecraft.util.maths.Vector2


trait Mission[TCommand] {
  type Executor = DroneControllerBase with MissionExecutor[TCommand]
  private[this] var assigned = Set.empty[Executor]
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
    assigned += hunter
    // println(s"$missionInstructions+ (${assigned.size}/$maxString)")
  }

  def relieve(hunter: Executor): Unit = {
    assigned -= hunter
    // println(s"$missionInstructions- (${assigned.size}/$maxString)")
  }

  def disband(): Unit = {
    for (a <- assigned) a.abortMission()
    // println(s"<$missionInstructions")
  }

  def findSuitableRecruits(candidates: Set[Executor]): Set[Executor] = {
    if (_isDeactivated) Set.empty
    else {
      val eligible =
        if (maxRequired == assigned.size) Set.empty[Executor]
        else candidates.
          filter(d => d.missionPriority < priority && candidateFilter(d))

      if (eligible.size + assigned.size < minRequired) Set.empty
      else {
        locationPreference match {
          case None => eligible.take(maxRequired - assigned.size)
          case Some(pos) =>
            eligible.toSeq.sortBy(d => (d.position - pos).lengthSquared).take(maxRequired - assigned.size).toSet
        }
      }
    }
  }

  def reduceAssignedToMax(): Unit = {
    while (maxRequired < nAssigned) {
      assigned.head.abortMission()
    }
  }

  def update(): Unit = ()
  def candidateFilter(drone: Drone): Boolean = true

  def deactivate(): Unit = {
    disband()
    _isDeactivated = true
  }

  def reactivate(): Unit = _isDeactivated = false

  def nAssigned: Int = assigned.size
}

trait MissionExecutor[TCommand] {
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

