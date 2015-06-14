package cwinter.codecraft.core.api

import java.io.File

import cwinter.codecraft.core.replay.{DummyDroneController, Replayer}
import cwinter.codecraft.core.{DroneWorldSimulator, SimulatorEvent, WorldMap, ai}
import cwinter.codecraft.graphics.application.DrawingCanvas
import cwinter.codecraft.util.maths.{Rng, Rectangle, Vector2}


object TheGameMaster {
  final val DefaultWorldSize = Rectangle(-4000, 4000, -2500, 2500)
  final val DefaultResourceDistribution = Seq(
      (20, 1), (20, 1), (20, 1), (20, 1),
      (20, 2), (20, 2),
      (15, 3), (15, 3),
      (15, 4), (15, 4)
    )


  def startGame(mothership1: DroneControllerBase, mothership2: DroneControllerBase): Unit = {
    val worldSize = DefaultWorldSize
    val resourceClusters = DefaultResourceDistribution
    val map = WorldMap(worldSize, resourceClusters, Seq(Vector2(2500, 500), Vector2(-2500, -500)))
    val simulator = new DroneWorldSimulator(map, mothership1, mothership2, devEvents)
    DrawingCanvas.run(simulator)
  }


  def runLevel1(mothership1: DroneControllerBase): Unit = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val map = WorldMap(worldSize, 100, Seq(Vector2(1000, 200), Vector2(-1000, -200)))
    val opponent = new ai.basic.Mothership()
    val simulator = new DroneWorldSimulator(map, mothership1, opponent, devEvents)
    DrawingCanvas.run(simulator)
  }


  def runReplay(filepath: String): Unit = {
    val replayer =
      new Replayer(scala.io.Source.fromFile(filepath).getLines())
    Rng.seed = replayer.seed
    val worldSize = replayer.worldSize
    val mineralCrystals = replayer.startingMinerals
    val spawns = replayer.spawns
    val map = WorldMap(mineralCrystals, worldSize, spawns)
    val mothership1 = new DummyDroneController
    val mothership2 = new DummyDroneController
    val simulator = new DroneWorldSimulator(map, mothership1, mothership2, devEvents, Some(replayer))
    DrawingCanvas.run(simulator)
  }

  def runLastReplay(): Unit = {
    val dir = new File(System.getProperty("user.home") + "/.codecraft/replays")
    val latest = dir.listFiles().maxBy(_.lastModified())
    runReplay(latest.getPath)
  }

  private var devEvents: Int => Seq[SimulatorEvent] = t => Seq()
  private[cwinter] def setDevEvents(generator: Int => Seq[SimulatorEvent]): Unit = {
    devEvents = generator
  }
}
