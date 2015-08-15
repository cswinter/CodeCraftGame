package cwinter.codecraft.core.api

import cwinter.codecraft.core.ai.cheese.CheesyMothership
import cwinter.codecraft.core._
import cwinter.codecraft.graphics.worldstate.{BluePlayer, OrangePlayer}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}


trait GameMasterLike {
  final val DefaultWorldSize = Rectangle(-4000, 4000, -2500, 2500)
  final val DefaultResourceDistribution = Seq(
      (20, 1), (20, 1), (20, 1), (20, 1),
      (20, 2), (20, 2),
      (15, 3), (15, 3),
      (15, 4), (15, 4)
    )
  final val DefaultMothership = new DroneSpec(
    missileBatteries = 2,
    constructors = 2,
    refineries = 3,
    storageModules = 3
  )


  private def constructSpawns(
    mothership1: DroneControllerBase,
    pos1: Vector2,
    mothership2: DroneControllerBase,
    pos2: Vector2
  ): Seq[Spawn] = {
    val spawn1 = new Spawn(DefaultMothership, mothership1, pos1, BluePlayer, 21)
    val spawn2 = new Spawn(DefaultMothership, mothership2, pos2, OrangePlayer, 21)
    Seq(spawn1, spawn2)
  }


  def run(simulator: DroneWorldSimulator): Unit

  def createSimulator(
    mothership1: DroneControllerBase,
    mothership2: DroneControllerBase,
    worldSize: Rectangle,
    resourceClusters: Seq[(Int, Int)],
    spawn1: Vector2,
    spawn2: Vector2
  ): Unit = {
    val spawns = constructSpawns(mothership1, spawn1, mothership2, spawn2)
    val map = WorldMap(worldSize, resourceClusters, spawns)
    new DroneWorldSimulator(map, devEvents)
  }

  def startGame(mothership1: DroneControllerBase, mothership2: DroneControllerBase): Unit = {
    val worldSize = DefaultWorldSize
    val resourceClusters = DefaultResourceDistribution
    val spawns = constructSpawns(mothership1, Vector2(2500, 500), mothership2, Vector2(-2500, -500))
    val map = WorldMap(worldSize, resourceClusters, spawns)
    val simulator = new DroneWorldSimulator(map, devEvents)
    run(simulator)
  }


  def runLevel1(mothership1: DroneControllerBase): Unit = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val spawns = constructSpawns(mothership1, Vector2(1000, 200), new ai.basic.Mothership, Vector2(-1000, -200))
    val map = WorldMap(worldSize, 100, spawns)
    val simulator = new DroneWorldSimulator(map, devEvents)
    run(simulator)
  }

  def runLevel2(mothership: DroneControllerBase): Unit = {
    startGame(mothership, new CheesyMothership)
  }

  def runLevel3(mothership: DroneControllerBase): Unit = {
    startGame(mothership, new ai.basicplus.Mothership)
  }

  def runL1vL2(): Unit = {
    startGame(new ai.basic.Mothership, new CheesyMothership)
  }

  def runL3vL3(): Unit = {
    startGame(new ai.basicplus.Mothership, new ai.basicplus.Mothership)
  }

  protected var devEvents: Int => Seq[SimulatorEvent] = t => Seq()
  protected[cwinter] def setDevEvents(generator: Int => Seq[SimulatorEvent]): Unit = {
    devEvents = generator
  }
}
