package cwinter.codinggame.demos.graphics

import cwinter.codinggame.worldstate._

object Chaos {
  val sightRadius = Some(250)
  val north = (math.Pi / 2).toFloat
  val customRobots = Seq(
    new MockRobot(
      xPos = 100,
      yPos = 0,
      orientation = north,
      modules = Seq(
        ProcessingModuleDescriptor(Seq(0, 1))
      ),
      size = 4,
      sightRadius = sightRadius
    ),
    new MockRobot(
      xPos = 200,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(Seq(0, 1, 2), 0, Some(1)),
        EnginesDescriptor(3)
      ),
      size = 5,
      sightRadius = sightRadius
    ),
    new MockRobot(
      xPos = 300,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(Seq(0, 1), 0),
        ProcessingModuleDescriptor(Seq(2, 3))
      ),
      size = 5,
      sightRadius = sightRadius
    ),
    new MockRobot(
      xPos = 400,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(Seq(0), 7),
        StorageModuleDescriptor(Seq(1, 2, 3), -1),
        ProcessingModuleDescriptor(Seq(4, 5, 6), mergingProgress = Some(1))
      ),
      size = 6,
      sightRadius = sightRadius
    ),
    new MockRobot(
      xPos = 550,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModuleDescriptor(Seq(0, 1), -1),
        StorageModuleDescriptor(Seq(2, 3, 4), -1),
        StorageModuleDescriptor(Seq(5, 6, 7, 8, 9), -1)
      ),
      size = 7,
      sightRadius = sightRadius
    )
  )

  def robotConstruction(time: Int): DroneDescriptor = {
    DroneDescriptor(
      identifier = -2,
      xPos = 100,
      yPos = 100,
      orientation = 2,
      positions = Seq(),
      modules = Seq(
        EnginesDescriptor(0),
        MissileBatteryDescriptor(1, 0),
        StorageModuleDescriptor(Seq(2), 0),
        ShieldGeneratorDescriptor(3),
        StorageModuleDescriptor(Seq(4), 0),
        MissileBatteryDescriptor(5,3),
        MissileBatteryDescriptor(6, 3)
      ),
      hullState = Seq[Byte](2, 2, 2, 2, 2),
      shieldState = Some(1),
      size = 6,
      BluePlayer,
      constructionState = Some(time),
      None, None
    )
  }


  def main(args: Array[String]): Unit = {
    val s = new GraphicsSimulator(
      customRobots,
      t => Seq(robotConstruction(t)),
      t => Seq(),
      sightRadius = sightRadius,
      nRandomDrones = 10,
      nRandomMinerals = 20,
      spawnProjectiles = true
    )
    s.run()
  }
}
