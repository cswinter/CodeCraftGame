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
      val (_, resources) = runFactory(factory, 2 * factory.MineralProcessingPeriod)
      assert(resources == -mineralSize * factory.MineralResourceYield)
    }
  }

  it should "generate a mineral destroyed event after processing a mineral crystal" in {
    for (mineralSize <- 1 to 5) {
      val mineralCrystal = new MineralCrystal(mineralSize, Vector2.NullVector, true)
      factory.startMineralProcessing(mineralCrystal)
      val (events, _) = runFactory(factory, 2 * factory.MineralProcessingPeriod)
      assert(events.contains(MineralCrystalDestroyed(mineralCrystal)))
    }
  }

  it should "not generate spurious events or resources" in {
    val (events, resources) = runFactory(factory, 250)
    assert(events == Seq())
    assert(resources == 0)
  }

  def runFactory(factory: DroneFactoryModule, minTime: Int): (Seq[SimulatorEvent], Int) = {
    var continue = true
    var allEvents = Seq.empty[SimulatorEvent]
    var netResources = 0
    while (continue) {
      continue = false
      for (i <- 0 until minTime) {
        val (events, r) = factory.update(0)
        if (r != 0 || events.nonEmpty) {
          continue = true
        }

        allEvents ++= events
        netResources += r
      }
    }

    (allEvents, netResources)
  }
}
