package cwinter.codecraft.core.ai.replicator.combat


object ScoutingMission extends Mission {
  val minRequired = 1
  val maxRequired = Int.MaxValue
  val missionInstructions = Scout
  val priority = 1
  val hasExpired = false
  val locationPreference = None
}
