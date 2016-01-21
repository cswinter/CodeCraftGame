package cwinter.codecraft.core.api

import java.io.File
import cwinter.codecraft.core.multiplayer.{JavaXWebsocketClient, WebsocketServerConnection}
import cwinter.codecraft.core.replay.{DummyDroneController, Replayer}
import cwinter.codecraft.core.{MultiplayerClientConfig, DroneWorldSimulator, WorldMap}
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


  def prepareMultiplayerGame(serverAddress: String, controller: DroneControllerBase): DroneWorldSimulator = {
    val websocketConnection = new JavaXWebsocketClient(s"ws$serverAddress:8080")
    val serverConnection = new WebsocketServerConnection(websocketConnection)
    val sync = serverConnection.receiveInitialWorldState()

    // TODO: receive this information from server
    val clientPlayers = Set[Player](BluePlayer)
    val serverPlayers = Set[Player](OrangePlayer)

    new DroneWorldSimulator(
      sync.worldMap,
      Seq(controller, new DummyDroneController),
      t => Seq.empty,
      None,
      MultiplayerClientConfig(clientPlayers, serverPlayers, serverConnection)
    )
  }
}

