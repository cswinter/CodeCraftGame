package cwinter.codecraft.core

import cwinter.codecraft.core.api._
import cwinter.codecraft.core.objects.drone.{HarvestMineral, DroneStorageModule, DroneImpl}
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.util.maths.{Vector2, Rectangle}
import org.scalatest.FlatSpec

class DroneWorldSimulatorTest extends FlatSpec {
  val mineral = new MineralCrystalImpl(1, Vector2(0, 0))
  val mockDroneSpec = new DroneSpec(storageModules = 2)
  val mockDrone =
    new DroneController {
      override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

      override def onTick(): Unit = ()

      override def onArrivesAtPosition(): Unit = ()

      override def onDeath(): Unit = ()

      override def onDroneEntersVision(drone: Drone): Unit = ()

      override def onSpawn(): Unit = ()
    }
  val map = new WorldMap(
    Seq(mineral), Rectangle(-2000, 2000, -2000, 2000),
    Seq(Spawn(mockDroneSpec, mockDrone, Vector2(0, 0), BluePlayer))
  )

  val simulator = new DroneWorldSimulator(map, _ => Seq())


  "Game simulator" must "allow for mineral harvesting" in {
    mockDrone.drone ! HarvestMineral(mineral)
    simulator.run(DroneStorageModule.HarvestingTime)
    assert(mineral.harvested)
  }

  it must "prevent double harvesting of resources" in {
    mockDrone.drone ! HarvestMineral(mineral)
    simulator.run(DroneStorageModule.HarvestingTime)
    assert(mockDrone.storedMinerals.size == 1)
  }
}
