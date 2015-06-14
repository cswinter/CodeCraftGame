package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.WorldConfig
import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import cwinter.codecraft.worldstate.BluePlayer
import org.scalatest.FlatSpec

class DroneShieldGeneratorModuleTest extends FlatSpec {
  val mockDroneSpec = DroneSpec(5, missileBatteries = 2, shieldGenerators = 1, engines = 1)
  val mockDrone = new DroneImpl(mockDroneSpec, null, BluePlayer, Vector2(0, 0), 0, WorldConfig(Rectangle(-100, 100, -100, 100)))
  val shieldGenerator = new DroneShieldGeneratorModule(Seq(2), mockDrone)


  "A shield generator module" should "absorb damage and loose hitpoints" in {
    val hitpointsBefore = shieldGenerator.currHitpoints
    val damage = 10
    val remainingDamage = shieldGenerator.absorbDamage(10)
    assert(remainingDamage < damage)
    assert(shieldGenerator.currHitpoints < hitpointsBefore)
    assert(shieldGenerator.currHitpoints + damage - remainingDamage == hitpointsBefore)
  }


  it should "regenerate all its hitpoints over time" in {
    DroneModuleTestHelper.multipleUpdates(shieldGenerator, 10000)
    assert(shieldGenerator.currHitpoints == shieldGenerator.maxHitpoints)
  }
}
