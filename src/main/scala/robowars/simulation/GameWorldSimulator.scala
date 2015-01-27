package robowars.simulation

import robowars.worldstate.{WorldObject, GameWorld}
import scala.util.Random


object GameWorldSimulator extends GameWorld {
  def rnd() = Random.nextDouble().toFloat
  def rnd(min: Float, max: Float): Float = {
    assert(min < max, "Cannot have min >= max.")
    rnd() * (max - min) + min
  }

  def rni(n: Int) = Random.nextInt(n)

  val minerals =
    for (i <- 0 to 20) yield
      new MockResource(
        2000 * rnd() - 1000,
        1000 * rnd() - 500,
        2 * math.Pi.toFloat * rnd(),
        rni(3) + 1)

  val robots =
    for (i <- 0 to 5) yield
      new MockRobot(
        2000 * rnd() - 1000,
        1000 * rnd() - 500,
        2 * math.Pi.toFloat * rnd(),
        1)

  var objects = collection.mutable.Set(minerals ++ robots:_*)


  def worldState: Iterable[WorldObject] = {
    objects.map(_.state())
  }

  def update(): Unit = {
    objects.foreach(_.update())
    objects.retain(!_.dead)
    if (rnd() < 0.01) {
      objects.add(new MockLightFlash(rnd(-500, 500), rnd(-200, 200)))
    }
  }
}
