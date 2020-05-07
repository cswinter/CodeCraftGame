package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.objects.HomingMissile
import cwinter.codecraft.util.maths.Vector2

private[core] trait DroneHull { self: DroneImpl =>
  private[this] var _hasDied: Boolean = false
  protected var hullState = List.fill[Byte](spec.sides - 1)(2)

  def missileHit(missile: HomingMissile): Unit = {
    if (context.isMultiplayerClient) return

    val multiplier = context.specialRules.mothershipDamageMultiplier
    val incomingDamage = if (multiplier != 1.0 && spec.constructors > 0) {
      multiplier.toInt + (if (context.rng.bernoulli(multiplier % 1)) 1 else 0)
    } else { 1 }
    val damage = shieldGenerators.fold(incomingDamage)(_.absorbDamage(incomingDamage))
    for (_ <- 0 until damage) hullState = damageHull(hullState)

    if (context.isLocallyComputed && context.isMultiplayer) {
      context.missileHits ::= MissileHit(id, missile.position, missile.id, incomingDamage - damage, damage)
    }

    missileHit(missile.position, incomingDamage)
  }

  def missileHit(position: Vector2, shieldDamage: Int, hullDamage: Int): Unit = {
    val remainingDamage = shieldGenerators.fold(shieldDamage)(_.absorbDamage(shieldDamage))
    assert(remainingDamage == 0, s"$hullDamage, $shieldDamage, $remainingDamage, $shieldGenerators")
    for (_ <- 0 until hullDamage) hullState = damageHull(hullState)

    missileHit(position, shieldDamage + hullDamage)
  }

  private def missileHit(position: Vector2, damage: Int): Unit = {
    if (hitpointsHull == 0) {
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
  def hitpoints: Int = hitpointsHull + shieldGenerators.fold(0)(_.currHitpoints)
  def hitpointsHull: Int = hullState.sum
}
