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
  override def run(simulator: DroneWorldSimulator): Unit = {
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
    val simulator = new DroneWorldSimulator(map, devEvents, Some(replayer))
    run(simulator)
  }

  def runLastReplay(): Unit = {
    val dir = new File(System.getProperty("user.home") + "/.codecraft/replays")
    val latest = dir.listFiles().maxBy(_.lastModified())
    runReplay(latest.getPath)
  }
}

