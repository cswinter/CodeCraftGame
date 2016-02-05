package cwinter.codecraft.demos.graphics

import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.{ColorRGB, Float0To1}

private[graphics] object Chaos {
  val sightRadius = Some(250)
  val north = (math.Pi / 2).toFloat
  val customDrones = Seq(
    new MockDrone(
      xPos = 100,
      yPos = 0,
      orientation = north,
      modules = Seq(
        ProcessingModuleDescriptor(Seq(0, 1))
      ),
      size = 4,
      sightRadius = sightRadius
    ),
    new MockDrone(
      xPos = 200,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(Seq(0, 1, 2), EmptyStorage, Some(1)),
        EnginesDescriptor(3)
      ),
      size = 5,
      sightRadius = sightRadius
    ),
    new MockDrone(
      xPos = 300,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(Seq(0, 1), EmptyStorage),
        ProcessingModuleDescriptor(Seq(2, 3))
      ),
      size = 5,
      sightRadius = sightRadius
    ),
    new MockDrone(
      xPos = 400,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(Seq(0), EnergyStorage()),
        StorageModuleDescriptor(Seq(1, 2, 3), MineralStorage),
        ProcessingModuleDescriptor(Seq(4, 5, 6), mergingProgress = Some(1))
      ),
      size = 6,
      sightRadius = sightRadius
    ),
    new MockDrone(
      xPos = 550,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(Seq(0, 1), MineralStorage),
        StorageModuleDescriptor(Seq(2, 3, 4), MineralStorage),
        StorageModuleDescriptor(Seq(5, 6, 7, 8, 9), MineralStorage)
      ),
      size = 7,
      sightRadius = sightRadius
    )
  )

  def droneConstruction(time: Int): DroneDescriptor = {
    DroneDescriptor(
      identifier = -2,
      xPos = 100,
      yPos = 100,
      orientation = 2,
      positions = Seq(),
      modules = Seq(
        EnginesDescriptor(0),
        MissileBatteryDescriptor(1, 0),
        StorageModuleDescriptor(Seq(2), EmptyStorage),
        ShieldGeneratorDescriptor(3),
        StorageModuleDescriptor(Seq(4), EmptyStorage),
        MissileBatteryDescriptor(5,3),
        MissileBatteryDescriptor(6, 3)
      ),
      hullState = Seq[Byte](2, 2, 2, 2, 2),
      shieldState = Some(1),
      size = 6,
      playerColor = ColorRGB(0, 0, 1),
      constructionState = Some(Float0To1(math.max(time / 5000f, 1))),
      None, None
    )
  }


  def main(args: Array[String]): Unit = {
    val s = new GraphicsSimulator(
      customDrones,
      t => Seq(droneConstruction(t)),
      t => Seq(),
      sightRadius = sightRadius,
      nRandomDrones = 10,
      nRandomMinerals = 20,
      spawnProjectiles = true
    )
    s.start()
  }
}
