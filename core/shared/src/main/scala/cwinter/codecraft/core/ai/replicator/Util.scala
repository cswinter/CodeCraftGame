package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.api.Drone


object Util {
  final val SoldierStrength = 2


  def approximateStrength(drone: Drone): Double = {
    val hitpoints = if (drone.isVisible) drone.hitpoints else drone.spec.maxHitpoints
    val attack = drone.spec.missileBatteries
    val strength = math.sqrt(attack * hitpoints)
    // 2 * the number of Soldier drones required to counter the shield regeneration
    val shieldRegenBonus = 2 * 0.3 * drone.spec.shieldGenerators
    // accounts for the fact that a larger drone has an advantage against several
    // smaller ones, since those will start to die off, decreasing the dps
    val sizeBonus = math.sqrt(strength / (0.5 * strength + 1))
    (strength * sizeBonus + shieldRegenBonus) / SoldierStrength
  }

  def approximateStrength(drones: Iterable[Drone]): Double =
    drones.foldLeft(0.0)((acc, d) => acc + approximateStrength(d))
}

