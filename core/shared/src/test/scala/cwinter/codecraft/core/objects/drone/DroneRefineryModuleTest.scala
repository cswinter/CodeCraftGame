package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.{BluePlayer, DroneSpec}
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.{WorldConfig, MineralCrystalDestroyed, SimulatorEvent}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import org.scalatest.FlatSpec

class DroneRefineryModuleTest extends FlatSpec {
  val mockDroneSpec = new DroneSpec(refineries = 5, storageModules = 2)
  val mockDrone = new DroneImpl(mockDroneSpec, null, BluePlayer, Vector2(0, 0), 0, WorldConfig(Rectangle(-100, 100, -100, 100)))

  val processingModule = new DroneRefineryModule((0 to 4).toSeq, mockDrone)

  "A refinery module" should "generate the correct amount of resources when processing a mineral crystal" in {
    for (mineralSize <- 1 to 5) {
      processingModule.startMineralProcessing(new MineralCrystalImpl(mineralSize, Vector2.Null, true))
      val (_, resourcesConsumed, resourcesSpawned) = runProcessingModule(processingModule, 2 * DroneRefineryModule.MineralProcessingPeriod)
      assert(resourcesSpawned.size == mineralSize * DroneRefineryModule.MineralResourceYield)
    }
  }

  it should "generate a mineral destroyed event after processing a mineral crystal" in {
    for (mineralSize <- 1 to 5) {
      val mineralCrystal = new MineralCrystalImpl(mineralSize, Vector2.Null, true)
      processingModule.startMineralProcessing(mineralCrystal)
      val (events, _, _) = runProcessingModule(processingModule, 2 * DroneRefineryModule.MineralProcessingPeriod)
      assert(events.contains(MineralCrystalDestroyed(mineralCrystal)))
    }
  }

  it should "not generate spurious events or resources" in {
    val (events, resourcesConsumed, resourcesSpawned) = runProcessingModule(processingModule, 250)
    assert(events == Seq())
    assert(resourcesConsumed.size == 0)
    assert(resourcesSpawned.size == 0)
  }

  it should "report the correct amount of unprocessed resources" in {
    processingModule.startMineralProcessing(new MineralCrystalImpl(3, Vector2.Null, true))
    processingModule.update(0)
    assert(processingModule.unprocessedResourceAmount == 3 * DroneRefineryModule.MineralResourceYield)
    for (i <- 0 until DroneRefineryModule.MineralProcessingPeriod / 3)
      processingModule.update(0)
    assert(processingModule.unprocessedResourceAmount == 3 * DroneRefineryModule.MineralResourceYield - 1)
  }

  private def runProcessingModule(module: DroneRefineryModule, minTime: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
    var continue = true
    var allEvents = Seq.empty[SimulatorEvent]
    var netResourceSpawns = Seq.empty[Vector2]
    var netResourceConsumption = Seq.empty[Vector2]
    while (continue) {
      continue = false
      for (i <- 0 until minTime) {
        val (events, r, rs) = module.update(0)
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
