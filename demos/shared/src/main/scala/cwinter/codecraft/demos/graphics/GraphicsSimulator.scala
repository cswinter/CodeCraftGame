package cwinter.codecraft.demos.graphics

import cwinter.codecraft.collisions.{VisionTracking, VisionTracker}
import cwinter.codecraft.graphics.engine.GraphicsEngine
import cwinter.codecraft.graphics.worldstate.{ModelDescriptor, NullPositionDescriptor, Simulator, TestingObject}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


private[graphics] class GraphicsSimulator(
  customObjects: Seq[MockObject],
  customChangingObjects: Int => Seq[ModelDescriptor[_]],
  spawnedObjects: Int => Seq[MockObject],
  val sightRadius: Option[Int] = None,
  nRandomDrones: Int = 0,
  nRandomMinerals: Int = 0,
  spawnProjectiles: Boolean = false
) extends Simulator {
  var time = 0

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
    } yield new MockDrone(xPos, yPos, orientation, size, modules)


  private def spawn(obj: MockObject): Unit = {
    objects.add(obj)
  }

  val objects = collection.mutable.Set(minerals ++ drones ++ customObjects:_*)

  override def computeWorldState: Iterable[ModelDescriptor[_]] = {
    objects.map(_.state()) + ModelDescriptor(NullPositionDescriptor, TestingObject(time), TestingObject(time)) ++ customChangingObjects(time)
  }

  override def update(): Unit = {
    time += 1

    objects.foreach(_.update())

    val missileExplosions = for {
      obj <- objects
      if obj.isInstanceOf[MockLaserMissile] && obj.dead
      missile = obj.asInstanceOf[MockLaserMissile]
    } yield new MockLightFlash(missile.xPos, missile.yPos)

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
