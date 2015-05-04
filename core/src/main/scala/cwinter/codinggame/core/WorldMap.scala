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


  def apply(size: Rectangle, resourceClusters: Int, clusterSize: Int): WorldMap = {

    val minerals =
      for {
        i <- 0 to resourceClusters
        m <- resourceCluster(0.75 * Rng.vector2(size), 150, clusterSize)
      } yield m

    WorldMap(size, minerals)
  }



  private def resourceCluster(origin: Vector2, spread: Double, amount: Int): Seq[MineralCrystal] = {
    assert(spread > 20)

    var positions = Vector(origin, origin)
    var minerals = Seq.empty[MineralCrystal]

    for {
      i <- 0 to amount
      origin = positions(Rng.int(positions.length))
      offset = Rng.double(20, spread) * Rng.vector2()
      pos = origin + offset
    } {
      positions :+= pos
      minerals :+= new MineralCrystal(Rng.int(1, 2), pos)
    }

    minerals
  }
}