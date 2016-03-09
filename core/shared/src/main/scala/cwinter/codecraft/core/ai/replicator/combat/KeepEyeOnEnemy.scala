package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.shared.Mission
import cwinter.codecraft.core.api.Drone


private[codecraft] class KeepEyeOnEnemy(enemy: Drone) extends Mission[ReplicatorCommand] {
  val minRequired = 1
  val maxRequired = 1
  val priority = 2

  def locationPreference = Some(enemy.lastKnownPosition)

  val missionInstructions = Observe(enemy, notFound)

  def notFound(): Unit = deactivate()

  override def update(): Unit = if (isDeactivated && enemy.isVisible) reactivate()

  def hasExpired = enemy.isDead
}

