package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.ReplicatorBase
import cwinter.codecraft.core.api.Drone


class Assist(
  val friend: ReplicatorBase,
  val priority: Int,
  radius: Int
) extends Mission {
  val radius2 = radius * radius
  var timeout = 10

  val minRequired = 1
  val maxRequired = Int.MaxValue

  def missionInstructions = AttackMove(
    if (friend.enemies.nonEmpty) friend.closestEnemy.lastKnownPosition
    else friend.position
  )
  def  hasExpired = timeout <= 0
  override def update(): Unit = timeout -= 1
  override def candidateFilter(drone: Drone): Boolean =
    (drone.position - friend.position).lengthSquared <= radius2

  def refresh(): Unit = timeout = 10
}
