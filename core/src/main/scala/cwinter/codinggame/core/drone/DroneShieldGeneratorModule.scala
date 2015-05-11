package cwinter.codinggame.core.drone

import cwinter.codinggame.core.SimulatorEvent
import cwinter.codinggame.worldstate.{DroneModuleDescriptor, ShieldGeneratorDescriptor}

class DroneShieldGeneratorModule(positions: Seq[Int], owner: Drone)
  extends DroneModule(positions, owner) {

  final val ShieldRegenPeriod = 100

  val nShieldGenerators: Int = positions.size
  val maxHitpoints: Int = nShieldGenerators * 7

  private[this] var regenCooldown: Int = ShieldRegenPeriod
  private[this] var _currHitpoints: Int = maxHitpoints
  def currHitpoints: Int = _currHitpoints

  override def update(availableResources: Int): (Seq[SimulatorEvent], Int) = {
    if (_currHitpoints < maxHitpoints) {
      regenCooldown = regenCooldown - 1
      if (regenCooldown == 0) {
        _currHitpoints = math.min(maxHitpoints, _currHitpoints + nShieldGenerators)
        regenCooldown = ShieldRegenPeriod
      }
    }

    NoEffects
  }


  def hitpointPercentage: Float = _currHitpoints.toFloat / maxHitpoints

  /**
   * Reduces shield strength by some amount of damage.
   * @return Returns the amount of damage which couldn't be absorbed.
   */
  def absorbDamage(damage: Int): Int = {
    val absorbed = math.min(damage, _currHitpoints)
    _currHitpoints -= absorbed
    damage - absorbed
  }

  override def descriptors: Seq[DroneModuleDescriptor] = positions.map(ShieldGeneratorDescriptor)
}

