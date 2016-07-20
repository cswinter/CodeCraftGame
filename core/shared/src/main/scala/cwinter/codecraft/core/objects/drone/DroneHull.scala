package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.objects.HomingMissile


private[core] trait DroneHull { self: DroneImpl =>
  private[this] var _hasDied: Boolean = false
  protected var hullState = List.fill[Byte](spec.sides - 1)(2)

  // TODO: mb only record hits here and do all processing as part of update()
  // NOTE: death state probable must be determined before (or at very start of) next update()
  def missileHit(missile: HomingMissile): Unit = {
    def damageHull(hull: List[Byte]): List[Byte] = hull match {
      case h :: hs =>
        if (h > 0) (h - 1).toByte :: hs
        else h :: damageHull(hs)
      case Nil => Nil
    }

    val incomingDamage = 1
    val damage = shieldGenerators.map(_.absorbDamage(incomingDamage)).getOrElse(incomingDamage)
    for (_ <- 0 until damage) hullState = damageHull(hullState)

    if (hitpoints == 0) {
      dynamics.remove()
      _hasDied = true
      for (s <- storage) s.droneHasDied()
    }

    addCollisionMarker(missile.position)
    invalidateModelCache()

    if (context.isLocallyComputed && context.isMultiplayer) {
      context.missileHits ::= MissileHit(id, missile.position, missile.id)
    }

    log(DamageTaken(damage, hitpoints))
  }


  override def isDead = _hasDied
  def hitpoints: Int = hullState.map(_.toInt).sum + shieldGenerators.map(_.currHitpoints).getOrElse(0)
}

