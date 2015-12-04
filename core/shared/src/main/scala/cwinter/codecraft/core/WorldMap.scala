package cwinter.codecraft.core

import cwinter.codecraft.core.api._
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.api.OrangePlayer
import cwinter.codecraft.util.maths.{Rng, Rectangle, Vector2}


/**
 * Defines the initial world state for a game.
 *
 * @param minerals The initial set of mineral crystals.
 * @param size The world boundary.
 * @param initialDrones The initial set of drones.
 * @param winCondition Win condition, if any.
 */
case class WorldMap(
  minerals: Seq[MineralCrystalImpl],
  size: Rectangle,
  initialDrones: Seq[Spawn],
  winCondition: Option[WinCondition] = None
) {
  // use this to get around compiler limitation (cannot have multiple overloaded methods with default arguments)
  private[codecraft] def withWinConditions(winCondition: WinCondition) = {
    WorldMap(minerals, size, initialDrones, Some(winCondition))
  }

  /**
   * Creates a copy of this WorldMap with the win conditions set to destruction of the enemy mothership.
   */
  def withDefaultWinConditions: WorldMap =
    this.withWinConditions(DestroyEnemyMotherships)
}

/**
 * Describes the initial position and state of a drone.
 *
 * @param droneSpec The specification for the modules and size of the drone.
 * @param controller A controller that will be assigned to the drone when the game starts.
 * @param position The initial position for the drone.
 * @param player The owner of the drone.
 * @param resources The amount of resources initially stored by the drone.
 * @param name Optional name by which the drone can be retrieved in the JavaScript version of the game.
 */
case class Spawn(
  droneSpec: DroneSpec,
  controller: DroneControllerBase,
  position: Vector2,
  player: Player,
  resources: Int = 0,
  name: Option[String] = None
)


object WorldMap {
  def apply(
    size: Rectangle,
    // use List instead of Seq to get around compiler limitation (cannot overload on type parameters)
    resources: List[(Vector2, Int)],
    initialDrones: Seq[Spawn]
  ): WorldMap = {
    val minerals =
      for ((pos, size) <- resources) yield
        new MineralCrystalImpl(size, pos)

    WorldMap(minerals, size, initialDrones)
  }

  def apply(
    size: Rectangle,
    resourceCount: Int,
    initialDrones: Seq[Spawn]
  ): WorldMap = {
    val minerals =
      for (i <- 0 to resourceCount) yield
      new MineralCrystalImpl(
        Rng.int(1, 2),
        new Vector2(Rng.double(size.xMin, size.xMax), Rng.double(size.yMin, size.yMax))
      )

    WorldMap(minerals, size, initialDrones)
  }


  def apply(
    size: Rectangle,
    resourceClusters: Seq[(Int, Int)],
    initialDrones: Seq[Spawn]
  ): WorldMap = {
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

    WorldMap(minerals, size, initialDrones)
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