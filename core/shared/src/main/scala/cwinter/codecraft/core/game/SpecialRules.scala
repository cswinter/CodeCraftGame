package cwinter.codecraft.core.game

case class SpecialRules(
  // Increases damage taken by mothership by this factor.
  // If not a whole integer, fractional damage is applied probabilistically.
  mothershipDamageMultiplier: Double = 1.0
)

object SpecialRules {
  def default: SpecialRules = SpecialRules()
}
