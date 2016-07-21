package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.objects.HomingMissile
import cwinter.codecraft.util.maths.Vector2


private[core] trait DroneHull { self: DroneImpl =>
  private[this] var _hasDied: Boolean = false
  protected var hullState = List.fill[Byte](spec.sides - 1)(2)

  // TODO: mb only record hits here and do all processing as part of update()
  // NOTE: death state probable must be determined before (or at very start of) next update()
  def missileHit(missile: HomingMissile): Unit = {
    val incomingDamage = 1
    val damage = shieldGenerators.map(_.absorbDamage(incomingDamage)).getOrElse(incomingDamage)
    for (_ <- 0 until damage) hullState = damageHull(hullState)

    if (context.isLocallyComputed && context.isMultiplayer) {
      context.missileHits ::= MissileHit(id, missile.position, missile.id, incomingDamage - damage, damage)
    }

    missileHit(missile.position, incomingDamage)
  }

  def missileHit(position: Vector2, shieldDamage: Int, hullDamage: Int): Unit = {
    val absorbed = shieldGenerators.map(_.absorbDamage(shieldDamage)).getOrElse(shieldDamage)
    assert(absorbed == 0, s"$hullDamage, $shieldDamage, $absorbed, $shieldGenerators")
    for (_ <- 0 until hullDamage) hullState = damageHull(hullState)

    missileHit(position, shieldDamage + hullDamage)
  }

  private def missileHit(position: Vector2, damage: Int): Unit = {
    if (hitpoints == 0) {
      dynamics.remove()
      _hasDied = true
      for (s <- storage) s.droneHasDied()
    }
    addCollisionMarker(position)
    log(DamageTaken(damage, hitpoints))
  }

  private def damageHull(hull: List[Byte]): List[Byte] = hull match {
    case h :: hs =>
      if (h > 0) (h - 1).toByte :: hs
      else h :: damageHull(hs)
    case Nil => Nil
  }

  override def isDead = _hasDied
  def hitpoints: Int = hullState.map(_.toInt).sum + shieldGenerators.map(_.currHitpoints).getOrElse(0)
}

