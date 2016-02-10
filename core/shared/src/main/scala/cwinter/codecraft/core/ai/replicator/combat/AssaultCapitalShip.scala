package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.shared.Mission
import cwinter.codecraft.core.api.Drone


class AssaultCapitalShip(enemy: Drone) extends Mission[ReplicatorCommand] {
  val minRequired = (enemy.spec.missileBatteries + 1) * (enemy.spec.shieldGenerators + 1)
  val maxRequired = minRequired * 2
  val priority = 10

  def locationPreference = Some(enemy.lastKnownPosition)

  private var searchRadius = 0.0

  def missionInstructions: ReplicatorCommand =
    if (enemy.isVisible || searchRadius == 0) Attack(enemy, this)
    else Search(enemy.lastKnownPosition, searchRadius)

  def notFound(): Unit = {
    searchRadius = 500
  }

  override def update(): Unit =
    if (!enemy.isVisible && nAssigned > 0 && searchRadius > 0) searchRadius += 1
    else if (enemy.isVisible) searchRadius = 0

  def hasExpired = enemy.isDead
}
