package cwinter.codinggame.core.drone

import cwinter.codinggame.core.{MineralCrystalDestroyed, SimulatorEvent, MineralCrystal}
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.BluePlayer
import org.scalatest.FlatSpec

class DroneFactoryModuleTest extends FlatSpec {
  val mockDrone = new Drone(
    Seq.fill(5)(NanobotFactory) ++ Seq.fill(2)(StorageModule),
    6, null, BluePlayer, Vector2(0, 0), 0)

  val factory = new DroneFactoryModule((0 to 4).toSeq, mockDrone)

  "A factory module" should "generate the correct amount of resources when processing a mineral crystal" in {
    for (mineralSize <- 1 to 5) {
      factory.startMineralProcessing(new MineralCrystal(mineralSize, Vector2.NullVector, true))
      val (_, resourcesConsumed, resourcesSpawned) = runFactory(factory, 2 * factory.MineralProcessingPeriod)
      assert(resourcesSpawned.size == mineralSize * factory.MineralResourceYield)
    }
  }

  it should "generate a mineral destroyed event after processing a mineral crystal" in {
    for (mineralSize <- 1 to 5) {
      val mineralCrystal = new MineralCrystal(mineralSize, Vector2.NullVector, true)
      factory.startMineralProcessing(mineralCrystal)
      val (events, _, _) = runFactory(factory, 2 * factory.MineralProcessingPeriod)
      assert(events.contains(MineralCrystalDestroyed(mineralCrystal)))
    }
  }

  it should "not generate spurious events or resources" in {
    val (events, resourcesConsumed, resourcesSpawned) = runFactory(factory, 250)
    assert(events == Seq())
    assert(resourcesConsumed.size == 0)
    assert(resourcesSpawned.size == 0)
  }

  def runFactory(factory: DroneFactoryModule, minTime: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var continue = true
    var allEvents = Seq.empty[SimulatorEvent]
    var netResourceSpawns = Seq.empty[Vector2]
    var netResourceConsumption = Seq.empty[Vector2]
    while (continue) {
      continue = false
      for (i <- 0 until minTime) {
        val (events, r, rs) = factory.update(0)
        if (r.nonEmpty || events.nonEmpty || rs.nonEmpty) {
          continue = true
        }

        allEvents ++= events
        netResourceConsumption ++= r
        netResourceSpawns ++= rs
      }
    }

    (allEvents, netResourceConsumption, netResourceSpawns)
  }
}
