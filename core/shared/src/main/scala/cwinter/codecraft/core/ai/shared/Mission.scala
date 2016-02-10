package cwinter.codecraft.core.ai.shared

import cwinter.codecraft.core.api.{Drone, DroneControllerBase}
import cwinter.codecraft.util.maths.Vector2


trait Mission[TCommand] {
  type Executor = DroneControllerBase with MissionExecutor[TCommand]
  private[this] var assigned = Set.empty[Executor]

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

  def reduceAssignedToMax(): Unit = {
    while (maxRequired < nAssigned) {
      assigned.head.abortMission()
    }
  }

  def update(): Unit = ()
  def candidateFilter(drone: Drone): Boolean = true

  def nAssigned: Int = assigned.size
}

trait MissionExecutor[TCommand] {
  def abortMission(): Unit
  def missionPriority: Int
  def startMission(mission: Mission[TCommand]): Unit
}

