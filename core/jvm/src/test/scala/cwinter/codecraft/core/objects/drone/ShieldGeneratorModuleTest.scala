package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.util.maths.Vector2
import org.scalatest.FlatSpec

class ShieldGeneratorModuleTest extends FlatSpec {
  val mockDroneSpec = DroneSpec(5, missileBatteries = 2, shieldGenerators = 1, engines = 1)
  val mockDrone = DroneFactory.blueDrone(mockDroneSpec, Vector2(0, 0))
  val shieldGenerator = new ShieldGeneratorModule(Seq(2), mockDrone)


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
