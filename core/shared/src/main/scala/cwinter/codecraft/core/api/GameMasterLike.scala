package cwinter.codecraft.core.api

import cwinter.codecraft.core.ai.cheese.CheesyMothership
import cwinter.codecraft.core.{DroneWorldSimulator, SimulatorEvent, WorldMap, ai}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}


trait GameMasterLike {
  final val DefaultWorldSize = Rectangle(-4000, 4000, -2500, 2500)
  final val DefaultResourceDistribution = Seq(
      (20, 1), (20, 1), (20, 1), (20, 1),
      (20, 2), (20, 2),
      (15, 3), (15, 3),
      (15, 4), (15, 4)
    )


  def run(simulator: DroneWorldSimulator): Unit

  def createSimulator(
    mothership1: DroneControllerBase,
    mothership2: DroneControllerBase,
    worldSize: Rectangle,
    resourceClusters: Seq[(Int, Int)],
    spawn1: Vector2,
    spawn2: Vector2
  ): Unit = {
    val map = WorldMap(worldSize, resourceClusters, Seq(spawn1, spawn2))
    new DroneWorldSimulator(map, mothership1, mothership2, devEvents)
  }

  def startGame(mothership1: DroneControllerBase, mothership2: DroneControllerBase): Unit = {
    val worldSize = DefaultWorldSize
    val resourceClusters = DefaultResourceDistribution
    val map = WorldMap(worldSize, resourceClusters, Seq(Vector2(2500, 500), Vector2(-2500, -500)))
    val simulator = new DroneWorldSimulator(map, mothership1, mothership2, devEvents)
    run(simulator)
  }


  def runLevel1(mothership1: DroneControllerBase): Unit = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val map = WorldMap(worldSize, 100, Seq(Vector2(1000, 200), Vector2(-1000, -200)))
    val opponent = new ai.basic.Mothership()
    val simulator = new DroneWorldSimulator(map, mothership1, opponent, devEvents)
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

  protected var devEvents: Int => Seq[SimulatorEvent] = t => Seq()
  protected[cwinter] def setDevEvents(generator: Int => Seq[SimulatorEvent]): Unit = {
    devEvents = generator
  }
}
