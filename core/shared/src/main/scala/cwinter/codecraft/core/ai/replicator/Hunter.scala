package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.util.maths.Rng


class Hunter(ctx: ReplicatorContext) extends ReplicatorBase('Hunter, ctx) {
  override def onTick(): Unit = {
    handleWeapons()

    if (enemies.nonEmpty) {
      val closest = closestEnemy
      moveInDirection(closest.position - position)
    } else {
      scout()
      if (searchToken.isEmpty && Rng.bernoulli(0.005)) {
        moveTo(0.9 * Rng.vector2(worldSize))
      }
    }
  }
}
