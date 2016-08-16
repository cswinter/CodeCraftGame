package cwinter.codecraft.core.ai.basicplus

import cwinter.codecraft.util.maths.{GlobalRNG, RNG, Vector2}

private[core] class Destroyer(val mothership: Mothership) extends BasicPlusController('Destroyer) {
  var attack = false
  var defend = false

  override def onSpawn(): Unit = {
    moveInDirection(Vector2(GlobalRNG.double(0, 100)))
  }

  override def onTick(): Unit = {
    handleWeapons()

    if (!defend && mothership.needsDefender) {
      mothership.registerDefender(this)
      defend = true
    }
    if (defend && mothership.allowsDefenderRelease) {
      mothership.unregisterDefender(this)
      defend = false
    }
    if (mothership.t % 600 == 0) attack = true

    if (enemies.nonEmpty) {
      val pClosest = closestEnemy.position
      if (canWin || defend) {
        moveInDirection(pClosest - position)
      } else {
        moveInDirection(position - pClosest)
        attack = false
      }
    } else if (defend) {
      if ((position - mothership.position).lengthSquared > 350 * 350) {
        moveTo(GlobalRNG.double(250, 350) * GlobalRNG.vector2() + mothership.position)
      }
    } else if (attack && mothership.lastCapitalShipSighting.isDefined) {
      for (p <- mothership.lastCapitalShipSighting)
        moveTo(p)
    } else if (GlobalRNG.bernoulli(0.005)) {
      moveTo(GlobalRNG.double(600, 900) * GlobalRNG.vector2() + mothership.position)
    }
  }

  override def onDeath(): Unit = {
    if (defend) mothership.unregisterDefender(this)
  }
}
