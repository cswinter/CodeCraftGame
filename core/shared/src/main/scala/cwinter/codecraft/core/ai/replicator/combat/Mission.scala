package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.Soldier
import cwinter.codecraft.core.api.Drone


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
