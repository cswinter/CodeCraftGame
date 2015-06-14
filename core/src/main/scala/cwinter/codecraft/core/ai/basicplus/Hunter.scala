package cwinter.codecraft.core.ai.basicplus

import cwinter.codecraft.util.maths.Rng

class Hunter(val mothership: Mothership) extends BaseController('Hunter) {
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

    if (Rng.bernoulli(0.005)) {
      moveTo(0.9 * Rng.vector2(worldSize))
    }
  }
}
