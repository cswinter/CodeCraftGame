package cwinter.codinggame.core.drone

import cwinter.codinggame.core.SpawnLaserMissile
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.{RedPlayer, BluePlayer}
import org.scalatest.FlatSpec


class DroneLasersModuleTest extends FlatSpec {
  val mockDrone = new Drone(Seq(Lasers, Lasers, Lasers, Engines), 5, null, BluePlayer, Vector2(0, 0), 0)
  val mockEnemy = new Drone(Seq(StorageModule), 3, null, RedPlayer, Vector2(100, 100), 0)

  "A laser module" should "not generate spurious events" in {
    val lasers = new DroneLasersModule(Seq(0, 1, 2), mockDrone)
    for {
      i <- 0 to 100
      event <- lasers.update(i)._1
    } fail()
  }

  it should "not consume resources" in {
    val lasers = new DroneLasersModule(Seq(0, 1, 2), mockDrone)
    for {
      i <- 0 to 100
      (events, resources) = lasers.update(i)
    } assert(resources == 0)
  }


  it should "generate missile events exactly once after firing" in {
    val lasers = new DroneLasersModule(Seq(0, 1, 2), mockDrone)

    assert(lasers.update(10) == ((Seq(), 0)))

    lasers.fire(mockEnemy)
    val (events, _) = lasers.update(10)
    assert(events.size == 3)
    assert(events.forall(_.isInstanceOf[SpawnLaserMissile]))

    val (events2, _) = lasers.update(10)
    assert(events2 == Seq())
  }
}
