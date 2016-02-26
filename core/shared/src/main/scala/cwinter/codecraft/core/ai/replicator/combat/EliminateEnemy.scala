package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.{ReplicatorContext, Util}
import cwinter.codecraft.core.ai.shared.Mission
import cwinter.codecraft.core.api.Drone


class EliminateEnemy(
  val enemy: Drone,
  val context: ReplicatorContext
) extends Mission[ReplicatorCommand] {
  var priority: Int = 3
  var minRequired = computeMinRequired
  def maxRequired = minRequired * 2

  def locationPreference = Some(enemy.lastKnownPosition)

  val missionInstructions: ReplicatorCommand = Attack(0, enemy, notFound)

  def notFound(): Unit = deactivate()

  override def update(): Unit = {
    if (isDeactivated && enemy.isVisible)
      reactivate()
    computePriority()
    val newMinRequired = computeMinRequired
    if (minRequired < newMinRequired) {
      minRequired = newMinRequired
      if (nAssigned < newMinRequired) disband()
    }
  }

  def computePriority(): Unit = {
    val motherships = context.mothershipCoordinator.motherships
    if (motherships.nonEmpty) {
      val closest = motherships.minBy(x => (x.position - enemy.lastKnownPosition).lengthSquared)
      val dist = (closest.position - enemy.lastKnownPosition).length
      priority = math.min(9, math.max(3, 9 - ((dist - 1000) / 250).toInt))
    }
  }

  def computeMinRequired: Int = math.ceil(
    if (clusters.contains(enemy)) clusters(enemy).strength
    else Util.approximateStrength(enemy)
  ).toInt

  def clusters = context.battleCoordinator.clusters

  def hasExpired = enemy.isDead
}
