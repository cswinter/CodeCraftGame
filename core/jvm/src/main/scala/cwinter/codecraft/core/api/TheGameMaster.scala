package cwinter.codecraft.core.api

import java.io.File

import cwinter.codecraft.core.{Settings, DroneWorldSimulator}
import cwinter.codecraft.core.multiplayer.{JavaXWebsocketClient, WebsocketClient}
import cwinter.codecraft.graphics.application.DrawingCanvas


/** Main entry point to start the game. */
object TheGameMaster extends GameMasterLike {
  override def run(simulator: DroneWorldSimulator): DroneWorldSimulator = {
    DrawingCanvas.run(simulator)
    simulator
  }

  /** Runs the replay stored in the specified file. */
  def runReplayFromFile(filepath: String): DroneWorldSimulator = {
    val simulator = createReplaySimulator(scala.io.Source.fromFile(filepath).mkString)
    run(simulator)
  }

  /** Runs the last recorded replay. */
  def runLastReplay(): DroneWorldSimulator = {
    val dir = new File(System.getProperty("user.home") + "/.codecraft/replays")
    val latest = dir.listFiles().maxBy(_.lastModified())
    runReplayFromFile(latest.getPath)
  }

  override def connectToWebsocket(connectionString: String): WebsocketClient =
    new JavaXWebsocketClient(connectionString)


  def lowFPS(): Unit = specificFPS(2)

  def forceGL2(): Unit = {
    Settings.default.forceGL2 = true
    val simulator = createSimulator(replicatorAI(), destroyerAI(), defaultMap)
    DrawingCanvas.run(simulator)
  }

  def specificFPS(fps: Int): Unit = {
    val simulator = createSimulator(replicatorAI(), destroyerAI(), defaultMap)
    DrawingCanvas.run(simulator, fps)
  }
}

