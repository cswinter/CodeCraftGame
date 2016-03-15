package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.Util
import cwinter.codecraft.core.ai.shared.Mission
import cwinter.codecraft.core.api.Drone


private[codecraft] class AssaultCapitalShip(enemy: Drone, context: ReplicatorBattleCoordinator)
extends Mission[ReplicatorCommand] {
  var minRequired = computeMinRequired
  def maxRequired = minRequired * 2
  val priority = 10
  var maxDist2: Option[Double] = None

  def locationPreference = Some(enemy.lastKnownPosition)

  private var searchRadius = 0.0

  def missionInstructions: ReplicatorCommand =
    if (enemy.isVisible || searchRadius == 0) Attack(maxDist2.getOrElse(0), enemy, notFound)
    else Search(enemy.lastKnownPosition, searchRadius)

  def notFound(): Unit = searchRadius = 750

  override def update(): Unit = {
    if (!enemy.isVisible && nAssigned > 0 && searchRadius > 0) searchRadius += 1
    else if (enemy.isVisible) searchRadius = 0

    maxDist2 =
      if (nAssigned == 0 || context.context.confident) None
      else {
        val sortedByDist =
          assigned.toSeq.sortBy(d => (enemy.lastKnownPosition - d.position).lengthSquared)
        val straggler = sortedByDist(math.min(nAssigned - 1, minRequired))
        Some((enemy.lastKnownPosition - straggler.position).length)
      }
  }

  def computeMinRequired: Int = math.ceil(
    if (context.clusters.contains(enemy)) context.clusters(enemy).strength
    else Util.approximateStrength(enemy)
  ).toInt

  def hasExpired = enemy.isDead
}
