package cwinter.codecraft.testai.replicator

import cwinter.codecraft.util.maths.Rng


class Hunter(ctx: ReplicatorContext) extends BaseController('Hunter, ctx) {
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
