package cwinter.codinggame.core

import cwinter.codinggame.util.maths.{Rng, Rectangle, Vector2}

case class WorldMap(
  size: Rectangle,
  minerals: Seq[MineralCrystal]
)


object WorldMap {
  def apply(size: Rectangle, resourceCount: Int): WorldMap = {
    val minerals =
      for (i <- 0 to resourceCount) yield
      new MineralCrystal(
        Rng.int(1, 2),
        new Vector2(Rng.double(size.xMin, size.xMax), Rng.double(size.yMin, size.yMax))
      )

    WorldMap(size, minerals)
  }
}