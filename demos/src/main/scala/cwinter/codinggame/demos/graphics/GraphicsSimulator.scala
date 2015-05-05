package cwinter.codinggame.demos.graphics

import cwinter.codinggame.collisions.VisionTracker
import cwinter.graphics.application.DrawingCanvas
import cwinter.worldstate._


class GraphicsSimulator(
  customObjects: Seq[MockObject],
  customChangingObjects: Int => Seq[WorldObjectDescriptor],
  spawnedObjects: Int => Seq[MockObject],
  val sightRadius: Option[Int] = None,
  nRandomDrones: Int = 0,
  nRandomMinerals: Int = 0,
  spawnProjectiles: Boolean = false
) extends GameWorld {
  var time = 0
  val vision = new VisionTracker[MockObject](-10000, 10000, -10000, 10000, sightRadius.getOrElse(250))


  import Generators._
  val minerals =
    for (i <- 0 until nRandomMinerals) yield
      new MockResource(
        2000 * rnd() - 1000,
        1000 * rnd() - 500,
        2 * math.Pi.toFloat * rnd(),
        rni(3) + 1)

  val drones =
    for {
      i <- 0 until nRandomDrones
      xPos = 2000 * rnd() - 1000
      yPos = 1000 * rnd() - 500
      orientation = 2 * math.Pi.toFloat * rnd()
      size = i % 5 + 3
      modules = randomModules(size)
    } yield new MockRobot(xPos, yPos, orientation, size, modules, sightRadius)


  private def spawn(obj: MockObject): Unit = {
    objects.add(obj)
    vision.insert(obj)
  }

  val objects = collection.mutable.Set(minerals ++ drones ++ customObjects:_*)
  objects.foreach(vision.insert(_))

  def worldState: Iterable[WorldObjectDescriptor] = {
    objects.map(_.state()) + TestingObject(time) ++ customChangingObjects(time)
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

    if (spawnProjectiles && rnd() < 0.03) {
      val missile = new MockLaserMissile(rnd(-500, 500), rnd(-250, 250), rnd(0, 2 * math.Pi.toFloat))
      spawn(missile)
    }

    spawnedObjects(time).foreach(spawn)
  }

  override def timestep = time

  def run(): Unit = {
    DrawingCanvas.run(this)
  }
}
