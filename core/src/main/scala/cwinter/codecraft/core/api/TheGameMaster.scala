package cwinter.codecraft.core.api

import java.io.File

import cwinter.codecraft.core.replay.{Replayer, DummyDroneController}
import cwinter.codecraft.core.{DroneWorldSimulator, SimulatorEvent, WorldMap, ai}
import cwinter.codecraft.graphics.application.DrawingCanvas
import cwinter.codecraft.util.maths.{Vector2, Rectangle}


object TheGameMaster {
  final val DefaultWorldSize = Rectangle(-4000, 4000, -2500, 2500)
  final val DefaultResourceDistribution = Seq(
      (20, 1), (20, 1), (20, 1), (20, 1),
      (20, 2), (20, 2),
      (15, 3), (15, 3),
      (15, 4), (15, 4)
    )


  def startGame(mothership1: DroneController, mothership2: DroneController): Unit = {
    val worldSize = DefaultWorldSize
    val resourceClusters = DefaultResourceDistribution
    val map = WorldMap(worldSize, resourceClusters, Seq(Vector2(2500, 500), Vector2(-2500, -500)))
    val simulator = new DroneWorldSimulator(map, mothership1, mothership2, devEvents)
    DrawingCanvas.run(simulator)
  }


  def runLevel1(mothership1: DroneController): Unit = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val map = WorldMap(worldSize, 100, Seq(Vector2(1000, 200), Vector2(-1000, -200)))
    val opponent = new ai.basic.Mothership()
    val simulator = new DroneWorldSimulator(map, mothership1, opponent, devEvents)
    DrawingCanvas.run(simulator)
  }


  def runReplay(filepath: String): Unit = {
    val replayer =
      new Replayer(scala.io.Source.fromFile(filepath).getLines())
    val worldSize = DefaultWorldSize
    val resourceClusters = DefaultResourceDistribution
    val spawns = replayer.spawns
    val map = WorldMap(worldSize, resourceClusters, spawns)
    val mothership1 = new DummyDroneController
    val mothership2 = new DummyDroneController
    val simulator = new DroneWorldSimulator(map, mothership1, mothership2, devEvents, Some(replayer))
    DrawingCanvas.run(simulator)
  }

  def runLastReplay(): Unit = {
    val dir = new File(System.getProperty("user.home") + "/.codecraft/replays")
    val latest = dir.listFiles().maxBy(_.lastModified())
    runReplay(latest.getCanonicalPath)
  }

  private var devEvents: Int => Seq[SimulatorEvent] = t => Seq()
  private[cwinter] def setDevEvents(generator: Int => Seq[SimulatorEvent]): Unit = {
    devEvents = generator
  }
}
