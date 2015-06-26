package cwinter.codecraft.core

import cwinter.codecraft.core.api.{Drone, DroneController, DroneSpec, MineralCrystal}
import cwinter.codecraft.core.objects.drone.{HarvestMineral, DroneStorageModule, DroneImpl}
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.graphics.worldstate.BluePlayer
import cwinter.codecraft.util.maths.{Vector2, Rectangle}
import org.scalatest.FlatSpec

class DroneWorldSimulatorTest extends FlatSpec {
  val mineral = new MineralCrystalImpl(1, Vector2(0, 0))
  val map = new WorldMap(Seq(mineral), Rectangle(-2000, 2000, -2000, 2000), Seq(Vector2(0, 0), Vector2(0, 0)))
  def emptyController = {
    println("created new drone controller")
    new DroneController {
      override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()

      override def onTick(): Unit = ()

      override def onArrivesAtPosition(): Unit = ()

      override def onDeath(): Unit = ()

      override def onDroneEntersVision(drone: Drone): Unit = ()

      override def onSpawn(): Unit = ()
    }
  }
  val mockDroneSpec = new DroneSpec(storageModules = 2)
  val mockDrone = new DroneImpl(mockDroneSpec, emptyController, BluePlayer, Vector2(0, 0), 0, WorldConfig(Rectangle(-100, 100, -100, 100)))

  val simulator = new DroneWorldSimulator(map, emptyController, emptyController, t => if (t == 0) Seq(SpawnDrone(mockDrone)) else Seq() )


  "Game simulator" must "allow for mineral harvesting" in {
    mockDrone.executeCommand(HarvestMineral(mineral))
    simulator.run(DroneStorageModule.HarvestingTime)
    assert(mineral.harvested)
  }

  it must "prevent double harvesting of resources" in {
    mockDrone.executeCommand(HarvestMineral(mineral))
    simulator.run(DroneStorageModule.HarvestingTime)
    assert(mockDrone.storedMinerals.size == 1)
  }
}
