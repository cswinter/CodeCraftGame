package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.shared.Mission

private[codecraft] class ScoutingMission extends Mission[ReplicatorCommand] {
  val minRequired = 1
  val maxRequired = Int.MaxValue
  val missionInstructions = Scout
  val priority = 1
  val hasExpired = false
  val locationPreference = None
}
