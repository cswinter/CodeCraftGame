package cwinter.codecraft.core.game

import cwinter.codecraft.core.api._
import cwinter.codecraft.util.maths.{GlobalRNG, Rectangle, Vector2}

/** Defines the initial world state for a game.
  *
  * @param minerals The initial set of mineral crystals.
  * @param size The world boundary.
  * @param initialDrones The initial set of drones.
  */
case class WorldMap(
  minerals: Seq[MineralSpawn],
  size: Rectangle,
  initialDrones: Seq[Spawn]
) {
  // use this to get around compiler limitation (cannot have multiple overloaded methods with default arguments)
  /*private[codecraft] def withWinConditions(winConditions: WinCondition*) = {
    WorldMap(minerals, size, initialDrones, winConditions)
  }*/

  def createGameConfig(droneControllers: Seq[DroneControllerBase],
                       winConditions: Seq[WinCondition] = defaultWinConditions,
                       tickPeriod: Int = 1,
                       rngSeed: Int = GlobalRNG.seed) =
    GameConfig(
      worldSize = size,
      minerals = minerals,
      drones = initialDrones zip droneControllers,
      winConditions = winConditions,
      tickPeriod = tickPeriod,
      rngSeed = rngSeed
    )

  private def defaultWinConditions = WinCondition.default
}

/** Describes the initial position and state of a drone.
  *
  * @param droneSpec The specification for the modules and size of the drone.
  * @param position The initial position for the drone.
  * @param player The owner of the drone.
  * @param resources The amount of resources initially stored by the drone.
  * @param name Optional name by which the drone can be retrieved in the JavaScript version of the game.
  */
case class Spawn(
  droneSpec: DroneSpec,
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
      for ((pos, size) <- resources) yield MineralSpawn(size, pos)

    WorldMap(minerals, size, initialDrones)
  }

  def apply(
    size: Rectangle,
    resourceCount: Int,
    initialDrones: Seq[Spawn]
  ): WorldMap = {
    val minerals =
      for (i <- 0 to resourceCount)
        yield
          MineralSpawn(
            GlobalRNG.int(1, 2),
            Vector2(GlobalRNG.double(size.xMin, size.xMax), GlobalRNG.double(size.yMin, size.yMax))
          )

    WorldMap(minerals, size, initialDrones)
  }

  def apply(
    size: Rectangle,
    resourceClusters: Seq[(Int, Int)],
    initialDrones: Seq[Spawn],
    symmetric: Boolean = false
  ): WorldMap = {
    var spread = 101
    var clusterPositions = List.empty[Vector2]
    var left = true
    def freshClusterPosition: Vector2 = {
      var cpos: Vector2 = null
      var fairPos: Vector2 = null
      do {
        cpos = 0.75 * GlobalRNG.vector2(size)
        fairPos =
          if (left) Vector2(math.abs(cpos.x), cpos.y)
          else Vector2(-math.abs(cpos.x), cpos.y)
        spread -= 1
      } while (clusterPositions.exists(p => (p - fairPos).lengthSquared <= 4 * 4 * spread * spread))
      clusterPositions ::= fairPos
      left = !left
      fairPos
    }

    val minerals =
      for {
        (mineralCount, size) <- resourceClusters
        m <- generateResourceCluster(freshClusterPosition,
                                     25,
                                     spread,
                                     mineralCount,
                                     GlobalRNG.int(1, size),
                                     symmetric)
      } yield m

    WorldMap(minerals, size, initialDrones)
  }

  private def generateResourceCluster(midpoint: Vector2,
                                      minDist: Double,
                                      spread: Double,
                                      amount: Int,
                                      maxSize: Int,
                                      symmetric: Boolean): Seq[MineralSpawn] = {
    var adjustedMinDist = minDist
    var minerals = Seq.empty[MineralSpawn]

    while (minerals.size < amount) {
      val pos = midpoint + spread * GlobalRNG.gaussian2D()
      val dist = (pos - midpoint).length
      val p = math.sqrt(math.exp(-dist * dist / 100000))
      val size = (GlobalRNG.double(0, p) * 3 * maxSize).toInt + 1
      if (!minerals.exists(
            m => (m.position - pos).lengthSquared <= adjustedMinDist * adjustedMinDist * size)) {
        minerals :+= MineralSpawn(size, pos)
        if (symmetric) minerals :+= MineralSpawn(size, Vector2(-pos.x, -pos.y))
      } else {
        adjustedMinDist *= .9
      }
    }

    minerals
  }
}

/** Describes the initial position and size of a mineral crystal. */
case class MineralSpawn(
  size: Int,
  position: Vector2
)
