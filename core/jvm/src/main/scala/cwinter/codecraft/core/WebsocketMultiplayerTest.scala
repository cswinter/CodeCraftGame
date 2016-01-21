package cwinter.codecraft.core

import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.multiplayer.{JavaXWebsocketClient, WebsocketServerConnection}
import cwinter.codecraft.core.replay.DummyDroneController


object WebsocketMultiplayerTest {
  def main(args: Array[String]): Unit = {
    new Thread {
      override def run(): Unit = {
        multiplayer.Server.spawnServerInstance()
      }
    }.start()

    Thread.sleep(2000, 0)
    val serverConnection = new WebsocketServerConnection(
      new JavaXWebsocketClient("ws://localhost:8080"))

    // TODO: receive this information from server
    val clientPlayers = Set[Player](BluePlayer)
    val serverPlayers = Set[Player](OrangePlayer)

    val sync = serverConnection.receiveInitialWorldState()

    val map = new WorldMap(
      sync.minerals,
      sync.worldSize,
      sync.initialDrones.map(_.deserialize),
      None
    )

    val client = new DroneWorldSimulator(
      map,
      Seq(TheGameMaster.level2AI(), new DummyDroneController),
      t => Seq.empty,
      None,
      MultiplayerClientConfig(clientPlayers, serverPlayers, serverConnection)
    )

    TheGameMaster.run(client)
  }
}
