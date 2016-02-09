package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.ReplicatorBase


class Guard(
  val friend: ReplicatorBase,
  var maxRequired: Int
) extends Mission {
  val priority = 10
  private var timeout = 0
  resetTimeout()

  def minRequired = maxRequired - 4

  def locationPreference = Some(friend.position)

  def missionInstructions = Circle(friend.position, 450)
  def hasExpired = maxRequired == 0 || friend.isDead
  override def update(): Unit = {
    timeout -= 1
    if (timeout == 0) {
      maxRequired -= 1
      reduceAssignedToMax()
      resetTimeout()
    }
  }

  private def resetTimeout(): Unit = timeout = 600

  def refresh(max: Int): Unit = {
    if (max > maxRequired) maxRequired = max
    else if (max + 1 >= maxRequired) resetTimeout()
  }
}

