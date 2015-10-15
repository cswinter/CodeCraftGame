package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.{WorldConfig, MineralCrystalHarvested}
import cwinter.codecraft.core.api.{BluePlayer, DroneSpec}
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import org.scalatest.FlatSpec

class DroneStorageModuleTest extends FlatSpec {
  val mockDrone1 = new DroneImpl(DroneSpec(storageModules = 4), null, BluePlayer, Vector2.Null, 0, WorldConfig(Rectangle(-100, 100, -100, 100)))
  val mockDrone2 = new DroneImpl(DroneSpec(storageModules = 2), null, BluePlayer, Vector2.Null, 0, WorldConfig(Rectangle(-100, 100, -100, 100)))
  val mineralCrystal = new MineralCrystalImpl(2, Vector2.Null)
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
