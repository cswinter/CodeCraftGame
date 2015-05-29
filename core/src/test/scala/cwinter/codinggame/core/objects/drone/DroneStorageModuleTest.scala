package cwinter.codinggame.core.objects.drone

import cwinter.codinggame.core.{WorldConfig, MineralCrystalHarvested}
import cwinter.codinggame.core.api.DroneSpec
import cwinter.codinggame.core.objects.MineralCrystal
import cwinter.codinggame.util.maths.{Rectangle, Vector2}
import cwinter.codinggame.worldstate.BluePlayer
import org.scalatest.FlatSpec

class DroneStorageModuleTest extends FlatSpec {
  val mockDrone1 = new Drone(DroneSpec(5, storageModules = 4), null, BluePlayer, Vector2.NullVector, 0, WorldConfig(Rectangle(-100, 100, -100, 100)))
  val mockDrone2 = new Drone(DroneSpec(4, storageModules = 2), null, BluePlayer, Vector2.NullVector, 0, WorldConfig(Rectangle(-100, 100, -100, 100)))
  val mineralCrystal = new MineralCrystal(2, Vector2.NullVector)
  val storageModule1 = mockDrone1.storage.get
  val storageModule2 = mockDrone2.storage.get

  "A drone storage module" should "harvest a mineral in less than 1000 timesteps" in {
    storageModule2.harvestMineral(mineralCrystal)

    val (events, _, _) = DroneModuleTestHelper.multipleUpdates(storageModule2, 1000)
    assert(events.contains(MineralCrystalHarvested(mineralCrystal)))
    assert(storageModule2.storedMinerals.contains(mineralCrystal))
  }


  it should "be able to deposit its mineral crystals in another storage module" in {
    mockDrone2 ! DepositMinerals(mockDrone1)

    for (i <- 0 to 1000) {
      mockDrone2.update()
      mockDrone1.update()
    }

    assert(!storageModule2.storedMinerals.contains(mineralCrystal))
    assert(storageModule1.storedMinerals.contains(mineralCrystal))
  }
}
