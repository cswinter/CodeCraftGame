package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.{WorldConfig, SpawnHomingMissile}
import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import cwinter.codecraft.worldstate.{RedPlayer, BluePlayer}
import org.scalatest.FlatSpec


class DroneMissileBatteryModuleTest extends FlatSpec {
  val mockDroneSpec = DroneSpec(missileBatteries = 3, engines = 1)
  val mockDrone = new DroneImpl(mockDroneSpec, null, BluePlayer, Vector2(0, 0), 0, WorldConfig(Rectangle(-100, 100, -100, 100)))
  val mockEnemySpec = DroneSpec(storageModules = 1)
  val mockEnemy = new DroneImpl(mockEnemySpec, null, RedPlayer, Vector2(100, 100), 0, WorldConfig(Rectangle(-100, 100, -100, 100)))

  "A laser module" should "not generate spurious events" in {
    val missileBattery = new DroneMissileBatteryModule(Seq(0, 1, 2), mockDrone)
    for {
      i <- 0 to 100
      event <- missileBattery.update(i)._1
    } fail()
  }

  it should "not consume resources" in {
    val lasers = new DroneMissileBatteryModule(Seq(0, 1, 2), mockDrone)
    for {
      i <- 0 to 100
      (events, resources, resourcesSpawned) = lasers.update(i)
    } assert(resources == Seq())
  }


  it should "generate missile events exactly once after firing" in {
    val lasers = new DroneMissileBatteryModule(Seq(0, 1, 2), mockDrone)

    assert(lasers.update(10) == ((Seq(), Seq(), Seq.empty[Vector2])))

    lasers.fire(mockEnemy)
    val (events, _, _) = lasers.update(10)
    assert(events.size == 3)
    assert(events.forall(_.isInstanceOf[SpawnHomingMissile]))

    val (events2, _, _) = lasers.update(10)
    assert(events2 == Seq())
  }
}
