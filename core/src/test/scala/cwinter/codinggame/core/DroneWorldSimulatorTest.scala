package cwinter.codinggame.core

import cwinter.codinggame.core.api.{DroneHandle, DroneController, DroneSpec, MineralCrystalHandle}
import cwinter.codinggame.core.objects.drone.{HarvestMineral, DroneStorageModule, Drone}
import cwinter.codinggame.core.objects.MineralCrystal
import cwinter.codinggame.util.maths.{Vector2, Rectangle}
import cwinter.codinggame.worldstate.BluePlayer
import org.scalatest.FlatSpec

class DroneWorldSimulatorTest extends FlatSpec {
  val mineral = new MineralCrystal(1, Vector2(0, 0))
  val map = new WorldMap(Seq(mineral), Rectangle(-2000, 2000, -2000, 2000), Seq(Vector2(0, 0), Vector2(0, 0)))
  def emptyController = {
    println("created new drone controller")
    new DroneController {
      override def onMineralEntersVision(mineralCrystal: MineralCrystalHandle): Unit = ()

      override def onTick(): Unit = ()

      override def onArrival(): Unit = ()

      override def onDeath(): Unit = ()

      override def onDroneEntersVision(drone: DroneHandle): Unit = ()

      override def onSpawn(): Unit = ()
    }
  }
  val mockDroneSpec = new DroneSpec(4, storageModules = 2)
  val mockDrone = new Drone(mockDroneSpec, emptyController, BluePlayer, Vector2(0, 0), 0, WorldConfig(Rectangle(-100, 100, -100, 100)))

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
