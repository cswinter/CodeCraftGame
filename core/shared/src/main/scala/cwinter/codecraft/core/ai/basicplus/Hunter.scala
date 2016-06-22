package cwinter.codecraft.core.ai.basicplus

import cwinter.codecraft.util.maths.GlobalRNG


private[core] class Hunter(val mothership: Mothership) extends BasicPlusController('Hunter) {
  override def onTick(): Unit = {
    handleWeapons()

    if (enemies.nonEmpty) {
      val closest = closestEnemy
      if (closest.spec.missileBatteries > 0) {
        if (!canWin) {
          moveInDirection(position - closest.position)
        }
      } else {
        moveInDirection(closest.position - position)
      }
    }

    if (GlobalRNG.bernoulli(0.005)) {
      moveTo(0.9 * GlobalRNG.vector2(worldSize))
    }
  }
}
