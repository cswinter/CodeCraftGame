package cwinter.codecraft.core.api

import cwinter.codecraft.core._
import cwinter.codecraft.core.ai.basicplus
import cwinter.codecraft.core.ai.cheese.Mothership
import cwinter.codecraft.core.game._
import cwinter.codecraft.core.multiplayer.{Error, FoundGame, ServerConnection}
import cwinter.codecraft.core.replay.Replayer
import cwinter.codecraft.util.maths.{Rectangle, Vector2}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

private[codecraft] trait GameMasterLike {

  /** Default dimensions for the size of the game world. */
  final val DefaultWorldSize = Rectangle(-3000, 3000, -2000, 2000)

  /** Smaller than default dimensions for the size of the game world. */
  final val SmallWorldSize = Rectangle(-2000, 2000, -1500, 1500)

  /** Larger than default dimensions for the size of the game world. */
  final val LargeWorldSize = Rectangle(-4500, 4500, -3000, 3000)

  /** Identifies the current version of the JavaScript API.
    * This value is incremented whenever a backwards incompatible change is made to the JavaScript API.
    */
  final val JavascriptAPIVersion = "0.2"

  private final val DefaultResourceDistribution = Seq(
    (10, 10),
    (10, 10),
    (7, 20),
    (7, 20),
    (5, 30),
    (5, 30),
    (5, 50),
    (5, 70),
    (5, 100)
  )

  private final val SmallResourceDistribution =
    for (group <- DefaultResourceDistribution.grouped(2).toSeq)
      yield group.head

  private final val LargeResourceDistribution =
    for ((n, size) <- DefaultResourceDistribution)
      yield (n, (size * 1.5).toInt)

  /** Default number of modules for the initial mothership. */
  final val DefaultMothership = DroneSpec(
    missileBatteries = 3,
    constructors = 3,
    storageModules = 3,
    shieldGenerators = 1
  )

  private def constructSpawns(pos1: Vector2, pos2: Vector2): Seq[Spawn] = {
    val spawn1 = Spawn(DefaultMothership, pos1, BluePlayer, 0)
    val spawn2 = Spawn(DefaultMothership, pos2, OrangePlayer, 0)
    Seq(spawn1, spawn2)
  }

  /** Runs the `simulator`. */
  def run(simulator: DroneWorldSimulator): DroneWorldSimulator

  /** Creates a new [[cwinter.codecraft.core.game.DroneWorldSimulator]] for a singleplayer game with the specified settings. */
  def createSimulator(
    mothership1: DroneControllerBase,
    mothership2: DroneControllerBase,
    worldSize: Rectangle,
    resourceClusters: Seq[(Int, Int)],
    spawn1: Vector2,
    spawn2: Vector2
  ): DroneWorldSimulator = {
    val spawns = constructSpawns(spawn1, spawn2)
    val map = WorldMap(worldSize, resourceClusters, spawns)
    val gameConfig = map.createGameConfig(Seq(mothership1, mothership2))
    new DroneWorldSimulator(gameConfig)
  }

  /** Creates a new [[cwinter.codecraft.core.game.DroneWorldSimulator]] for a singleplayer game with the specified settings. */
  def createSimulator(
    mothership1: DroneControllerBase,
    mothership2: DroneControllerBase,
    map: WorldMap
  ): DroneWorldSimulator =
    new DroneWorldSimulator(map.createGameConfig(Seq(mothership1, mothership2)))

  /** Creates a new drone world simulator from a replay string. */
  def createReplaySimulator(replayText: String): DroneWorldSimulator = {
    val replayer = new Replayer(replayText.lines)
    new DroneWorldSimulator(replayer.gameConfig)
  }

  /** Starts a new game with two players.
    *
    * @param mothership1 The controller for the initial mothership of player 1.
    * @param mothership2 The controller for the initial mothership of player 2.
    */
  @deprecated("This method has been renamed to `runGame` and will be removed in a future version.",
              "0.2.4.3")
  def startGame(mothership1: DroneControllerBase, mothership2: DroneControllerBase): DroneWorldSimulator =
    runGame(mothership1, mothership2)

  /** Runs a game with default settings.
    *
    * @param mothership1 The drone controller for player 1.
    * @param mothership2 The drone controller for player 2.
    */
  def runGame(mothership1: DroneControllerBase, mothership2: DroneControllerBase): DroneWorldSimulator = {
    val controllers = Seq(mothership1, mothership2)
    val simulator = new DroneWorldSimulator(defaultMap.createGameConfig(controllers))
    run(simulator)
    simulator
  }

  /** Returns a drone controller for the level 1 AI. */
  def level1AI(): DroneControllerBase = new ai.basic.Mothership

  /** Returns a drone controller for the level 2 AI. */
  def level2AI(): DroneControllerBase = new basicplus.Mothership

  /** Returns a drone controller for the level 3 AI. */
  def bonusLevelAI(): DroneControllerBase = new ai.cheese.Mothership

  /** Returns a drone controller for the level 4 AI */
  def level4AI(): DroneControllerBase =
    replicatorAI(aggressive = true, confident = true)

  /** Returns a drone controller for the level 4 AI */
  def level5AI(): DroneControllerBase = destroyerAI()

  /** Returns a drone controller for the level 4 AI */
  def level6AI(): DroneControllerBase =
    replicatorAI(greedy = true, confident = true)

  /** Returns a drone controller for the level 4 AI */
  def level7AI(): DroneControllerBase = replicatorAI()

  /** Returns a drone controller for the Replicator AI. */
  def replicatorAI(greedy: Boolean = false,
                   confident: Boolean = false,
                   aggressive: Boolean = false): DroneControllerBase =
    new ai.replicator.Replicator(greedy, confident, aggressive)

  /** Returns a drone controller for the Destroyer AI. */
  def destroyerAI(): DroneControllerBase =
    new ai.destroyer.DestroyerContext().mothership

  /** The default [[cwinter.codecraft.core.game.WorldMap]]. */
  def defaultMap: WorldMap = {
    val spawns = constructSpawns(Vector2(2500, 500), Vector2(-2500, -500))
    WorldMap(DefaultWorldSize, DefaultResourceDistribution, spawns)
  }

  /** A small [[cwinter.codecraft.core.game.WorldMap]]. */
  def smallMap: WorldMap = {
    val spawns = constructSpawns(Vector2(1650, 500), Vector2(-1650, -500))
    WorldMap(SmallWorldSize, SmallResourceDistribution, spawns)
  }

  /** A large [[cwinter.codecraft.core.game.WorldMap]]. */
  def largeMap: WorldMap = {
    val spawns = constructSpawns(Vector2(3800, 1000), Vector2(-3800, -1000))
    WorldMap(LargeWorldSize, LargeResourceDistribution, spawns)
  }

  /** The [[cwinter.codecraft.core.game.WorldMap]] for the first level. */
  def level1Map: WorldMap = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val spawns = constructSpawns(Vector2(1000, 200), Vector2(-1000, -200))
    WorldMap(worldSize, Seq.fill(8)((2, 40)), spawns)
  }

  /** The [[cwinter.codecraft.core.game.WorldMap]] for the second level. */
  def level2Map: WorldMap = defaultMap

  /** The [[cwinter.codecraft.core.game.WorldMap]] for the bonus level. */
  def bonusLevelMap: WorldMap = defaultMap

  /** Runs the first level.
    *
    * @param mothership1 The controller for your mothership.
    */
  def runLevel1(mothership1: DroneControllerBase): DroneWorldSimulator = {
    val map = level1Map
    val controllers = Seq(mothership1, level1AI())
    val simulator = new DroneWorldSimulator(map.createGameConfig(controllers))
    run(simulator)
    simulator
  }

  /** Runs the second level.
    *
    * @param mothership The controller for your mothership.
    */
  def runLevel2(mothership: DroneControllerBase) =
    runGame(mothership, level2AI())

  /** Runs the third level.
    *
    * @param mothership The controller for your mothership.
    */
  def runLevel3(mothership: DroneControllerBase) =
    runGame(mothership, bonusLevelAI())

  /** Runs the fourth level.
    *
    * @param mothership The controller for your mothership.
    */
  def runLevel4(mothership: DroneControllerBase) =
    runGame(mothership, level4AI())

  /** Runs the fifth level.
    *
    * @param mothership The controller for your mothership.
    */
  def runLevel5(mothership: DroneControllerBase) =
    runGame(mothership, level5AI())

  /** Runs the sixth level.
    *
    * @param mothership The controller for your mothership.
    */
  def runLevel6(mothership: DroneControllerBase) =
    runGame(mothership, level6AI())

  /** Runs the seventh level.
    *
    * @param mothership The controller for your mothership.
    */
  def runLevel7(mothership: DroneControllerBase) =
    runGame(mothership, level7AI())

  /** Runs a game with the level 1 AI versus the level 2 AI. */
  def runL1vL2(): DroneWorldSimulator =
    runGame(new ai.basic.Mothership, new Mothership)

  /** Runs a game with the level 3 AI versus the level 3 AI. */
  def runL3vL3(): DroneWorldSimulator =
    runGame(new ai.basicplus.Mothership, new ai.basicplus.Mothership)

  /** Sets up a multiplayer game with the specified server. */
  def prepareMultiplayerGame(
    serverAddress: String,
    controller: DroneControllerBase
  ): Future[DroneWorldSimulator] =
    prepareMultiplayerGame(serverAddress).map(_(controller))

  /** Sets up a multiplayer game with the specified server. */
  def prepareMultiplayerGame(
    serverAddress: String
  ): Future[DroneControllerBase => DroneWorldSimulator] = {
    val resultPromise = Promise[DroneControllerBase => DroneWorldSimulator]
    val serverConnection = new ServerConnection(
      serverAddress,
      onStateTransition = {
        case FoundGame(conn) => resultPromise.complete(Success(conn))
        case Error(x) =>
          if (!resultPromise.isCompleted) resultPromise.complete(Failure(x))
          else {
            println(s"Additional failure: $x")
            x.printStackTrace()
          }
        case _ =>
      }
    )
    serverConnection.connect()
    resultPromise.future
  }
}
