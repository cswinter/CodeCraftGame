package cwinter.codinggame.core

import cwinter.codinggame.util.maths.{Rng, Rectangle, Vector2}

case class Map(
  size: Rectangle,
  minerals: Seq[MineralCrystal]
)


object Map {
  def apply(size: Rectangle, resourceCount: Int): Map = {
    val minerals =
      for (i <- 0 to resourceCount) yield
      new MineralCrystal(
        Rng.int(1, 3),
        new Vector2(Rng.double(size.xMin, size.xMax), Rng.double(size.yMin, size.yMax))
      )

    Map(size, minerals)
  }
}