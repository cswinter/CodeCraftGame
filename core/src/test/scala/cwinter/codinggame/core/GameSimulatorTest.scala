package cwinter.codinggame.core

import cwinter.codinggame.core.drone.{DroneStorageModule, StorageModule, Drone, DroneController}
import cwinter.codinggame.util.maths.{Vector2, Rectangle}
import cwinter.codinggame.worldstate.BluePlayer
import org.scalatest.FlatSpec

class GameSimulatorTest extends FlatSpec {
  val mineral = new MineralCrystal(1, Vector2(0, 0))
  val map = new WorldMap(Rectangle(-2000, 2000, -2000, 2000), Seq(mineral))
  val emptyController = new DroneController {
    override def onMineralEntersVision(mineralCrystal: MineralCrystal): Unit = ()
    override def onTick(): Unit = ()
    override def onArrival(): Unit = ()
    override def onDeath(): Unit = ()
    override def onDroneEntersVision(drone: Drone): Unit = ()
    override def onSpawn(): Unit = ()
  }
  val mockDrone = new Drone(Seq(StorageModule, StorageModule), 4, emptyController, BluePlayer, Vector2(0, 0), 0, 0)

  val simulator = new GameSimulator(map, emptyController, emptyController, t => if (t == 0) Seq(SpawnDrone(mockDrone)) else Seq())


  "Game simulator" must "allow for mineral harvesting" in {
    mockDrone.harvestResource(mineral)
    for (_ <- 0 until DroneStorageModule.HarvestingTime) {
      simulator.update()
    }
    assert(mineral.harvested)
  }

  it must "prevent double harvesting of resources" in {
    mockDrone.harvestResource(mineral)
    for (_ <- 0 until DroneStorageModule.HarvestingTime) {
      simulator.update()
    }
    assert(mockDrone.storedMinerals.size == 1)
  }
}
