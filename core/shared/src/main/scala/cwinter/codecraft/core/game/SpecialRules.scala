package cwinter.codecraft.core.game

import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.util.maths.RNG

case class SpecialRules(
  // Increases damage taken by mothership by this factor.
  // If not a whole integer, fractional damage is applied probabilistically.
  mothershipDamageMultiplier: Double = 1.0,
  costModifierSize: Array[Double] = Array(1.0, 1.0, 1.0, 1.0),
  costModifierMissiles: Double = 1.0,
  costModifierShields: Double = 1.0,
  costModifierStorage: Double = 1.0,
  costModifierConstructor: Double = 1.0,
  costModifierEngines: Double = 1.0
) {
  private[codecraft] def modifiedCost(rng: RNG, spec: DroneSpec): Int = {
    val sizeModifier = if (spec.moduleCount - 1 < costModifierSize.length) {
      costModifierSize(spec.moduleCount - 1)
    } else {
      1.0
    }
    def discretize(double: Double): Int = {
      val fractional = if (rng.bernoulli(double - double.floor)) 1 else 0
      double.floor.toInt + fractional
    }
    def moduleCost(count: Int, modifier: Double): Int = {
      import cwinter.codecraft.core.api.GameConstants.ModuleResourceCost
      val amount = modifier * sizeModifier * count * ModuleResourceCost
      discretize(amount)
    }

    moduleCost(spec.missileBatteries, costModifierMissiles) +
      moduleCost(spec.shieldGenerators, costModifierShields) +
      moduleCost(spec.storageModules, costModifierStorage) +
      moduleCost(spec.constructors, costModifierConstructor) +
      moduleCost(spec.engines, costModifierEngines)
  }

}

object SpecialRules {
  def default: SpecialRules = SpecialRules()
}
