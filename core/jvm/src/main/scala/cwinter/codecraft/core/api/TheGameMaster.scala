package cwinter.codecraft.core.api

import java.io.File
import cwinter.codecraft.core.replay.Replayer
import cwinter.codecraft.core.{DroneWorldSimulator, WorldMap}
import cwinter.codecraft.graphics.application.DrawingCanvas
import cwinter.codecraft.util.maths.Rng


/**
 * Main entry point to start the game.
 */
object TheGameMaster extends GameMasterLike {
  override def run(simulator: DroneWorldSimulator): DroneWorldSimulator = {
    DrawingCanvas.run(simulator)
    simulator
  }

  def runReplayFromFile(filepath: String): DroneWorldSimulator = {
    val simulator = createReplaySimulator(scala.io.Source.fromFile(filepath).mkString)
    run(simulator)
  }

  def runLastReplay(): DroneWorldSimulator = {
    val dir = new File(System.getProperty("user.home") + "/.codecraft/replays")
    val latest = dir.listFiles().maxBy(_.lastModified())
    runReplayFromFile(latest.getPath)
  }
}

