package cwinter.codecraft.demos.graphics

import cwinter.codecraft.collisions.VisionTracker
import cwinter.codecraft.graphics.engine.GraphicsEngine
import cwinter.codecraft.graphics.worldstate.{ModelDescriptor, Simulator, TestingObject, WorldObjectDescriptor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


private[graphics] class GraphicsSimulator(
  customObjects: Seq[MockObject],
  customChangingObjects: Int => Seq[ModelDescriptor],
  spawnedObjects: Int => Seq[MockObject],
  val sightRadius: Option[Int] = None,
  nRandomDrones: Int = 0,
  nRandomMinerals: Int = 0,
  spawnProjectiles: Boolean = false
) extends Simulator {
  var time = 0
  val vision = new VisionTracker[MockObject](-10000, 10000, -10000, 10000, sightRadius.getOrElse(250))


  import Generators._

  override protected def asyncUpdate(): Future[Unit] = Future {
    update()
  }

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
    } yield new MockDrone(xPos, yPos, orientation, size, modules, sightRadius)


  private def spawn(obj: MockObject): Unit = {
    objects.add(obj)
    vision.insert(obj)
  }

  val objects = collection.mutable.Set(minerals ++ drones ++ customObjects:_*)
  objects.foreach(vision.insert(_))

  override def computeWorldState: Iterable[ModelDescriptor] = {
    objects.map(_.state()) + ModelDescriptor(0, 0, 0, TestingObject(time)) ++ customChangingObjects(time) ++
      objects.flatMap{case drone: MockDrone => drone.extraState() case _ => Seq()}
  }

  override def update(): Unit = {
    time += 1

    objects.foreach(_.update())
    vision.updateAll()
    for (r <- objects) { r match {
      case drone: MockDrone => drone.inSight = vision.getVisible(r)
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

  
  def start(): Unit = {
    GraphicsEngine.run(this)
  }
}
