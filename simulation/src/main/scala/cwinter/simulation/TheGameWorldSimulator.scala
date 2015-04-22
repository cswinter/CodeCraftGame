package cwinter.simulation

import cwinter.graphics.application.DrawingCanvas
import cwinter.worldstate._
import scala.util.Random
import cwinter.collisions.VisionTracker


object TheGameWorldSimulator extends GameWorld {
  val SightRadius = 250
  var time = 0
  val vision = new VisionTracker[MockObject](-10000, 10000, -10000, 10000, SightRadius)

  def rnd() = Random.nextDouble().toFloat
  def rnd(min: Float, max: Float): Float = {
    assert(min < max, "Cannot have min >= max.")
    rnd() * (max - min) + min
  }

  def rni(n: Int) = if (n <= 0) 0 else Random.nextInt(n)

  def rnd[T](elems: (Int, T)*): T = {
    val totalWeight = elems.map(_._1).sum
    val r = rni(totalWeight)
    var cumulativeWeight = 0
    var i = -1
    do {
      i += 1
      cumulativeWeight += elems(i)._1
    } while (cumulativeWeight < r)
    elems(i)._2
  }

  def randomModule(position: Int) = rnd(
    50 -> StorageModule(Seq(position), rni(9) - 1),
    2 -> Lasers(position, rni(4)),
    2 -> Engines(position),
    2 -> ShieldGenerator(position),
    6 -> ProcessingModule(Seq(position))
  )

  val ModuleCount = Map(3 -> 1, 4 -> 2, 5 -> 4, 6 -> 7, 7 -> 10).withDefaultValue(0)
  def randomModules(n: Int) = {
    Seq.tabulate(ModuleCount(n))(i => randomModule(i))
  }

  val minerals =
    for (i <- 0 to 20) yield
      new MockResource(
        2000 * rnd() - 1000,
        1000 * rnd() - 500,
        2 * math.Pi.toFloat * rnd(),
        rni(3) + 1)

  val robots =
    for {
      i <- 0 to 10
      xPos = 2000 * rnd() - 1000
      yPos = 1000 * rnd() - 500
      orientation = 2 * math.Pi.toFloat * rnd()
      size = i % 5 + 3
      modules = randomModules(size)
    } yield new MockRobot(xPos, yPos, orientation, size, modules)

  val north = (math.Pi / 2).toFloat
  val customRobots = Seq(
    new MockRobot(
      xPos = 100,
      yPos = 0,
      orientation = north,
      modules = Seq(
        ProcessingModule(Seq(0, 1))
      ),
      size = 4
    ),
    new MockRobot(
      xPos = 200,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModule(Seq(0, 1, 2), 0, Some(1)),
        Engines(3)
      ),
      size = 5
    ),
    new MockRobot(
      xPos = 300,
      yPos = 0,
      orientation = north,
      modules = Seq(
        StorageModule(Seq(0, 1), 0),
        ProcessingModule(Seq(2, 3))
      ),
      size = 5
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
      size = 6
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
      size = 7
    )
  )


  val objects = collection.mutable.Set(minerals ++ robots ++ customRobots:_*)
  objects.foreach(vision.insert(_))

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
      constructionState = Some(time),
      None, None
    )
  }

  def worldState: Iterable[WorldObjectDescriptor] = {
    objects.map(_.state()) + TestingObject(time) + robotConstruction(time)
  }

  def update(): Unit = {
    time += 1

    objects.foreach(_.update())
    vision.updateAll()
    for (r <- objects) { r match {
      case robot: MockRobot => robot.inSight = vision.getVisible(r)
      case _ =>
    } }

    val missileExplosions = for {
      obj <- objects
      if obj.isInstanceOf[MockLaserMissile] && obj.dead
      missile = obj.asInstanceOf[MockLaserMissile]
    } yield {
        vision.remove(missile)
        new MockLightFlash(missile.xPos, missile.yPos)
      }

    objects ++= missileExplosions

    objects.retain(!_.dead)

    if (rnd() < 0.03) {
      val missile = new MockLaserMissile(rnd(-500, 500), rnd(-250, 250), rnd(0, 2 * math.Pi.toFloat))
      objects.add(missile)
      vision.insert(missile)
    }
  }

  override def timestep = time

  def main(args: Array[String]): Unit = {
    DrawingCanvas.run(this)
  }
}
