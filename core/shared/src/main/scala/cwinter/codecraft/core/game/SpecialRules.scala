package cwinter.codecraft.core.game

import cwinter.codecraft.core.api.GameConstants.ModuleResourceCost
import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.util.maths.RNG

sealed case class SpecialRules(
  // Increases damage taken by mothership by this factor.
  // If not a whole integer, fractional damage is applied probabilistically.
  mothershipDamageMultiplier: Double = 1.0,
  unitCostModifiers: Map[DroneSpec, Double] = Map.empty
) {
  private[codecraft] def modifiedCost(rng: RNG, spec: DroneSpec): Int = {
    def discretize(double: Double): Int = {
      val fractional = if (rng.bernoulli(double - double.floor)) 1 else 0
      double.floor.toInt + fractional
    }
    unitCostModifiers.get(spec) match {
      case Some(modifier) => discretize(spec.moduleCount * ModuleResourceCost * modifier)
      case None => spec.moduleCount * ModuleResourceCost
    }
  }
}

object SpecialRules {
  def default: SpecialRules = SpecialRules()
}
