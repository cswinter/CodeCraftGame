package cwinter.codinggame.core

import cwinter.codinggame.core.objects.MineralCrystal
import cwinter.codinggame.util.maths.{Rng, Rectangle, Vector2}

case class WorldMap(
  minerals: Seq[MineralCrystal],
  size: Rectangle,
  spawns: Seq[Vector2]
)


object WorldMap {
  def apply(size: Rectangle, resourceCount: Int, spawns: Seq[Vector2]): WorldMap = {
    val minerals =
      for (i <- 0 to resourceCount) yield
      new MineralCrystal(
        Rng.int(1, 2),
        new Vector2(Rng.double(size.xMin, size.xMax), Rng.double(size.yMin, size.yMax))
      )

    WorldMap(minerals, size, spawns)
  }


  def apply(size: Rectangle, resourceClusters: Seq[(Int, Int)], spawns: Seq[Vector2]): WorldMap = {
    val spread = 100
    var clusterPositions = List.empty[Vector2]
    def freshClusterPosition: Vector2 = {
      var cpos: Vector2 = null
      do {
        cpos = 0.75 * Rng.vector2(size)
      } while (clusterPositions.exists(p => (p - cpos).magnitudeSquared <= 60 * spread))
      clusterPositions ::= cpos
      cpos
    }

    val minerals =
      for {
        (mineralCount, size) <- resourceClusters
        m <- generateResourceCluster(freshClusterPosition, 25, spread, mineralCount, Rng.int(1, size))
      } yield m

    WorldMap(minerals, size, spawns)
  }



  private def generateResourceCluster(midpoint: Vector2, minDist: Double, spread: Double, amount: Int, maxSize: Int): Seq[MineralCrystal] = {
    var minerals = Seq.empty[MineralCrystal]

    while (minerals.size < amount) {
      val pos = midpoint + spread * Rng.gaussian2D()
      val dist = (pos - midpoint).size
      val p = math.sqrt(math.exp(-dist * dist / 100000))
      val size = (Rng.double(0, p) * 3 * maxSize).toInt + 1
      if (!minerals.exists(m => (m.position - pos).magnitudeSquared <= minDist * minDist * size)) {
        minerals :+= new MineralCrystal(size, pos)
      }
    }

    minerals
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