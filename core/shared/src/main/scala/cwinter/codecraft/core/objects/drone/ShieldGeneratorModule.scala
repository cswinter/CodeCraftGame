package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.SimulatorEvent
import cwinter.codecraft.core.api.GameConstants.{ShieldMaximumHitpoints, ShieldRegenerationInterval}
import cwinter.codecraft.core.graphics.{DroneModuleDescriptor, ShieldGeneratorDescriptor}
import cwinter.codecraft.util.maths.Vector2


private[core] class ShieldGeneratorModule(positions: Seq[Int], owner: DroneImpl)
  extends DroneModule(positions, owner) {

  val nShieldGenerators: Int = positions.size
  val maxHitpoints: Int = nShieldGenerators * ShieldMaximumHitpoints

  private[this] var regenCooldown: Int = ShieldRegenerationInterval
  private[this] var _currHitpoints: Int = maxHitpoints
  def currHitpoints: Int = _currHitpoints

  override def update(availableResources: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    if (_currHitpoints < maxHitpoints) {
      regenCooldown = regenCooldown - 1
      if (regenCooldown == 0) {
        if (_currHitpoints != maxHitpoints) owner.mustUpdateModel()
        _currHitpoints = math.min(maxHitpoints, _currHitpoints + nShieldGenerators)
        regenCooldown = ShieldRegenerationInterval
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
    owner.mustUpdateModel()
    val absorbed = math.min(damage, _currHitpoints)
    _currHitpoints -= absorbed
    damage - absorbed
  }

  override def descriptors: Seq[DroneModuleDescriptor] = positions.map(ShieldGeneratorDescriptor)
}

