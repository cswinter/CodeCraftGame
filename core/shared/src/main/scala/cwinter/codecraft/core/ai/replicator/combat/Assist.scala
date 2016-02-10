package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.ReplicatorController
import cwinter.codecraft.core.api.Drone


class Assist(
  val friend: ReplicatorController,
  val priority: Int,
  val minRequired: Int,
  radius: Int
) extends Mission {
  val radius2 = radius * radius
  var timeout = 10

  val maxRequired = Int.MaxValue
  val locationPreference = None

  def missionInstructions = AttackMove(
    if (friend.enemies.nonEmpty) friend.closestEnemy.lastKnownPosition
    else friend.position
  )
  def  hasExpired = friend.isDead || timeout <= 0
  override def update(): Unit = timeout -= 1
  override def candidateFilter(drone: Drone): Boolean =
    drone != friend &&
    (drone.position - friend.position).lengthSquared <= radius2

  def refresh(): Unit = timeout = 10
}
