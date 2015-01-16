package robowars.simulation

import robowars.worldstate.{MineralObject, WorldObject, GameWorld}

import scala.util.Random


object GameWorldSimulator extends GameWorld {
  def rnd() = Random.nextDouble().toFloat
  def rni(n: Int) = Random.nextInt(n)

  val objects =
    for (i <- 0 to 20) yield
      MineralObject(
        i,
        2000 * rnd() - 1000,
        1000 * rnd() - 500,
        2 * math.Pi.toFloat * rnd(),
        rni(4))


  def worldState: Iterable[WorldObject] =
    objects
}
