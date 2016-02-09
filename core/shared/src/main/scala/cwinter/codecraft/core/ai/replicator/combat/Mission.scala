package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.Soldier
import cwinter.codecraft.core.api.Drone
import cwinter.codecraft.util.maths.Vector2


trait Mission {
  private[this] var assigned = Set.empty[Soldier]

  def minRequired: Int
  def maxRequired: Int
  def missionInstructions: MissionInstructions
  def priority: Int
  def hasExpired: Boolean
  def locationPreference: Option[Vector2]

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
