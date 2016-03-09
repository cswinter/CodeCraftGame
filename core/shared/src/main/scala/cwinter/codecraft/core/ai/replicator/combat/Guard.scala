package cwinter.codecraft.core.ai.replicator.combat

import cwinter.codecraft.core.ai.replicator.ReplicatorController
import cwinter.codecraft.core.ai.shared.Mission


private[codecraft] class Guard(
  val friend: ReplicatorController,
  var minRequired: Int
) extends Mission[ReplicatorCommand] {
  val priority = 10
  private var timeout = 0
  resetTimeout()

  def maxRequired = (minRequired * 1.5f).toInt

  def locationPreference = Some(friend.position)

  def missionInstructions = Circle(friend.position, 450)
  def hasExpired = maxRequired == 0 || friend.isDead
  override def update(): Unit = {
    timeout -= 1
    if (timeout == 0) {
      minRequired -= 1
      reduceAssignedToMax()
      resetTimeout()
    }
  }

  private def resetTimeout(): Unit = timeout = 600

  def refresh(min: Int): Unit = {
    if (min > minRequired) minRequired = min
    else if (min + 1 >= minRequired) resetTimeout()
  }
}

