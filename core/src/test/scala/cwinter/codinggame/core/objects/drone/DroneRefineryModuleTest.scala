package cwinter.codinggame.core.objects.drone

import cwinter.codinggame.core.api.DroneSpec
import cwinter.codinggame.core.objects.MineralCrystal
import cwinter.codinggame.core.{MineralCrystalDestroyed, SimulatorEvent}
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.BluePlayer
import org.scalatest.FlatSpec

private[core] class DroneRefineryModuleTest extends FlatSpec {
  val mockDroneSpec = new DroneSpec(6, refineries = 5, storageModules = 2)
  val mockDrone = new Drone(mockDroneSpec, null, BluePlayer, Vector2(0, 0), 0)

  val processingModule = new DroneRefineryModule((0 to 4).toSeq, mockDrone)

  "A factory module" should "generate the correct amount of resources when processing a mineral crystal" in {
    for (mineralSize <- 1 to 5) {
      processingModule.startMineralProcessing(new MineralCrystal(mineralSize, Vector2.NullVector, true))
      val (_, resourcesConsumed, resourcesSpawned) = runProcessingModule(processingModule, 2 * processingModule.MineralProcessingPeriod)
      assert(resourcesSpawned.size == mineralSize * processingModule.MineralResourceYield)
    }
  }

  it should "generate a mineral destroyed event after processing a mineral crystal" in {
    for (mineralSize <- 1 to 5) {
      val mineralCrystal = new MineralCrystal(mineralSize, Vector2.NullVector, true)
      processingModule.startMineralProcessing(mineralCrystal)
      val (events, _, _) = runProcessingModule(processingModule, 2 * processingModule.MineralProcessingPeriod)
      assert(events.contains(MineralCrystalDestroyed(mineralCrystal)))
    }
  }

  it should "not generate spurious events or resources" in {
    val (events, resourcesConsumed, resourcesSpawned) = runProcessingModule(processingModule, 250)
    assert(events == Seq())
    assert(resourcesConsumed.size == 0)
    assert(resourcesSpawned.size == 0)
  }

  def runProcessingModule(module: DroneRefineryModule, minTime: Int): (Seq[SimulatorEvent], Seq[Vector2], Seq[Vector2]) = {
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
