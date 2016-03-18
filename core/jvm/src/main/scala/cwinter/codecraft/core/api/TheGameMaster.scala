package cwinter.codecraft.core.api

import java.io.File

import cwinter.codecraft.core.multiplayer.{WebsocketClient, JavaXWebsocketClient, WebsocketServerConnection}
import cwinter.codecraft.core.{DroneWorldSimulator, MultiplayerClientConfig, MultiplayerConfig, WorldMap}
import cwinter.codecraft.graphics.application.DrawingCanvas

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


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
}

