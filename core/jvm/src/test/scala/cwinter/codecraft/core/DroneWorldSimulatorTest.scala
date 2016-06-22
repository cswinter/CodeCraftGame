package cwinter.codecraft.core

import cwinter.codecraft.core.api._
import cwinter.codecraft.core.objects.drone.HarvestMineral
import cwinter.codecraft.core.replay.DummyDroneController
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import org.scalatest.{FlatSpec, Matchers}

class DroneWorldSimulatorTest extends FlatSpec with Matchers {
  val mineralSpawn = new MineralSpawn(1, Vector2(0, 0))
  val mockDroneSpec = new DroneSpec(storageModules = 2)
  val mockDrone = new DummyDroneController

  val map = new WorldMap(
    Seq(mineralSpawn), Rectangle(-2000, 2000, -2000, 2000),
    Seq(Spawn(mockDroneSpec, Vector2(0, 0), BluePlayer))
  )

  val simulator = new DroneWorldSimulator(map, Seq(mockDrone), _ => Seq())
  val mineral = simulator.minerals.head


  "Game simulator" must "allow for mineral harvesting" in {
    mockDrone.drone ! HarvestMineral(mineral)
    simulator.run(GameConstants.HarvestingInterval)
    assert(mineral.harvested)
  }

  it must "prevent double harvesting of resources" in {
    mockDrone.drone ! HarvestMineral(mineral)
    simulator.run(GameConstants.HarvestingInterval)
    mockDrone.storedResources shouldBe 1
  }
}
