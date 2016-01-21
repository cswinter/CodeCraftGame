package cwinter.codecraft.core

import cwinter.codecraft.core.api.{Player, TheGameMaster, BluePlayer, OrangePlayer}
import cwinter.codecraft.core.multiplayer.{LocalServerConnection, LocalClientConnection, LocalConnection}
import cwinter.codecraft.core.replay.DummyDroneController


object IntraRuntimeMultiplayerTest {
  def main(args: Array[String]): Unit = {
    val clientPlayers = Set[Player](BluePlayer)
    val serverPlayers = Set[Player](OrangePlayer)
    val map = TheGameMaster.defaultMap()
    val connection = new LocalConnection(Set(0))
    val clientConnection0 = new LocalClientConnection(0, connection, clientPlayers)
    val serverConnection = new LocalServerConnection(0, connection)
    val server = new DroneWorldSimulator(
      map,
      Seq(new DummyDroneController, TheGameMaster.level1AI()),
      t => Seq.empty,
      None,
      AuthoritativeServerConfig(serverPlayers, clientPlayers, Set(clientConnection0))
    )
    val client = new DroneWorldSimulator(
      map,
      Seq(TheGameMaster.level2AI(), new DummyDroneController),
      t => Seq.empty,
      None,
      MultiplayerClientConfig(clientPlayers, serverPlayers, serverConnection)
    )
    val displayClient = true
    if (displayClient) {
      server.run()
      TheGameMaster.run(client)
    } else {
      client.run()
      TheGameMaster.run(server)
    }
  }
}

