package cwinter.codecraft.core

import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.util.maths.{Rng, Rectangle, Vector2}

case class WorldMap(
  minerals: Seq[MineralCrystalImpl],
  size: Rectangle,
  spawns: Seq[Vector2]
)


object WorldMap {
  def apply(size: Rectangle, resourceCount: Int, spawns: Seq[Vector2]): WorldMap = {
    val minerals =
      for (i <- 0 to resourceCount) yield
      new MineralCrystalImpl(
        Rng.int(1, 2),
        new Vector2(Rng.double(size.xMin, size.xMax), Rng.double(size.yMin, size.yMax))
      )

    WorldMap(minerals, size, spawns)
  }


  def apply(size: Rectangle, resourceClusters: Seq[(Int, Int)], spawns: Seq[Vector2]): WorldMap = {
    val spread = 100
    var clusterPositions = List.empty[Vector2]
    var left = true
    def freshClusterPosition: Vector2 = {
      var cpos: Vector2 = null
      var fairPos: Vector2 = null
      do {
        cpos = 0.75 * Rng.vector2(size)
        fairPos =
          if (left) Vector2(math.abs(cpos.x), cpos.y)
          else Vector2(-math.abs(cpos.x), cpos.y)
      } while (clusterPositions.exists(p => (p - fairPos).lengthSquared <= 4 * 4 * spread * spread))
      clusterPositions ::= fairPos
      left = !left
      fairPos
    }

    val minerals =
      for {
        (mineralCount, size) <- resourceClusters
        m <- generateResourceCluster(freshClusterPosition, 25, spread, mineralCount, Rng.int(1, size))
      } yield m

    WorldMap(minerals, size, spawns)
  }



  private def generateResourceCluster(midpoint: Vector2, minDist: Double, spread: Double, amount: Int, maxSize: Int): Seq[MineralCrystalImpl] = {
    var minerals = Seq.empty[MineralCrystalImpl]

    while (minerals.size < amount) {
      val pos = midpoint + spread * Rng.gaussian2D()
      val dist = (pos - midpoint).length
      val p = math.sqrt(math.exp(-dist * dist / 100000))
      val size = (Rng.double(0, p) * 3 * maxSize).toInt + 1
      if (!minerals.exists(m => (m.position - pos).lengthSquared <= minDist * minDist * size)) {
        minerals :+= new MineralCrystalImpl(size, pos)
      }
    }

    minerals
  }
}