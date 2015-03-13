package robowars.simulation

import robowars.worldstate.{StorageModule, WorldObject, GameWorld}
import scala.util.Random


object TheGameWorldSimulator extends GameWorld {
  def rnd() = Random.nextDouble().toFloat
  def rnd(min: Float, max: Float): Float = {
    assert(min < max, "Cannot have min >= max.")
    rnd() * (max - min) + min
  }

  def rni(n: Int) = if (n <= 0) 0 else Random.nextInt(n)

  def randomModule = StorageModule(rni(7))

  val ModuleCount = Map(3 -> 1, 4 -> 3, 5 -> 6, 6 -> 9).withDefaultValue(0)
  def randomModules(n: Int) = {
    Seq.fill(ModuleCount(n))(randomModule)
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


  val objects = collection.mutable.Set(minerals ++ robots:_*)


  def worldState: Iterable[WorldObject] = {
    objects.map(_.state())
  }

  def update(): Unit = {
    objects.foreach(_.update())

    val missileExplosions = for {
      obj <- objects
      if obj.isInstanceOf[MockLaserMissile] && obj.dead
      missile = obj.asInstanceOf[MockLaserMissile]
    } yield new MockLightFlash(missile.xPos, missile.yPos)
    objects ++= missileExplosions

    objects.retain(!_.dead)

    if (rnd() < 0.1) {
      objects.add(new MockLaserMissile(rnd(-500, 500), rnd(-250, 250), rnd(0, 2 * math.Pi.toFloat)))
    }
  }
}
