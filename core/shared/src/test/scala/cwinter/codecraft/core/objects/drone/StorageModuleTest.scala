package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.MineralCrystalHarvested
import cwinter.codecraft.core.api.DroneSpec
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.util.maths.Vector2
import org.scalatest.{Matchers, FlatSpec}

class StorageModuleTest extends FlatSpec with Matchers {
  val mockDrone1 = DroneFactory.blueDrone(DroneSpec(storageModules = 4), Vector2.Null)
  val mockDrone2 = DroneFactory.blueDrone(DroneSpec(storageModules = 2), Vector2.Null)
  val mineralSize = 2
  val mineralCrystal = new MineralCrystalImpl(mineralSize, 0, Vector2.Null)
  val storageModule1 = mockDrone1.storage.get
  val storageModule2 = mockDrone2.storage.get
  val mineralCrystal2 = new MineralCrystalImpl(100, 1, Vector2.Null)

  "A drone storage module" should "harvest a mineral in less than 1000 timesteps" in {
    storageModule2.harvestMineral(mineralCrystal)

    val (events, _, _) = DroneModuleTestHelper.multipleUpdates(storageModule2, 1000)
    events should contain(MineralCrystalHarvested(mineralCrystal))
    storageModule2.storedResources shouldBe mineralSize
  }


  it should "be able to deposit its resources in another storage module" in {
    mockDrone2 ! DepositMinerals(mockDrone1)

    for (i <- 0 to 1000) {
      mockDrone2.update()
      mockDrone1.update()
    }

    storageModule2.storedResources shouldBe 0
    storageModule1.storedResources shouldBe mineralSize
  }

  it should "not be able to harvest a mineral that is already being harvested by another drone" in {
    storageModule1.harvestMineral(mineralCrystal2)
    mockDrone1.update()

    storageModule1.isHarvesting shouldBe true

    storageModule2.harvestMineral(mineralCrystal2)
    mockDrone2.update()

    storageModule1.isHarvesting shouldBe true
    storageModule2.isHarvesting shouldBe false
  }
}
