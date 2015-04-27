package cwinter.codinggame.demos.graphics

import cwinter.worldstate._

object Chaos {
  val sightRadius = Some(250)
  val north = (math.Pi / 2).toFloat
  val customRobots = Seq(
    new MockRobot(
      xPos = 100,
      yPos = 0,
      orientation = north,
      modules = Seq(
        ProcessingModule(Seq(0, 1))
      ),
      size = 4,
      sightRadius = sightRadius
    ),
    new MockRobot(
      xPos = 200,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModule(Seq(0, 1, 2), 0, Some(1)),
        Engines(3)
      ),
      size = 5,
      sightRadius = sightRadius
    ),
    new MockRobot(
      xPos = 300,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModule(Seq(0, 1), 0),
        ProcessingModule(Seq(2, 3))
      ),
      size = 5,
      sightRadius = sightRadius
    ),
    new MockRobot(
      xPos = 400,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModule(Seq(0), 7),
        StorageModule(Seq(1, 2, 3), -1),
        ProcessingModule(Seq(4, 5, 6), mergingProgress = Some(1))
      ),
      size = 6,
      sightRadius = sightRadius
    ),
    new MockRobot(
      xPos = 550,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModule(Seq(0, 1), -1),
        StorageModule(Seq(2, 3, 4), -1),
        StorageModule(Seq(5, 6, 7, 8, 9), -1)
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
        Engines(0),
        Lasers(1, 0),
        StorageModule(Seq(2), 0),
        ShieldGenerator(3),
        StorageModule(Seq(4), 0),
        Lasers(5,3),
        Lasers(6, 3)
      ),
      hullState = Seq[Byte](2, 2, 2, 2, 2),
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
