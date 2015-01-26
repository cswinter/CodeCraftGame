package robowars.simulation

import robowars.worldstate.{WorldObject, GameWorld}
import scala.util.Random


object GameWorldSimulator extends GameWorld {
  def rnd() = Random.nextDouble().toFloat

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

  val objects = minerals ++ robots


  def worldState: Iterable[WorldObject] = {
    objects.map(_.state())
  }

  def update(): Unit = {
    objects.foreach(_.update())
  }
}
