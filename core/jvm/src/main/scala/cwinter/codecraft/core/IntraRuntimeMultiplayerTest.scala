package cwinter.codecraft.core

import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.game.{AuthoritativeServerConfig, DroneWorldSimulator, MultiplayerClientConfig}
import cwinter.codecraft.core.multiplayer.{LocalClientConnection, LocalConnection, LocalServerConnection}
import cwinter.codecraft.core.replay.DummyDroneController

import scala.concurrent.duration._

private[codecraft] object IntraRuntimeMultiplayerTest {
  def main(args: Array[String]): Unit = {
    val clientPlayers = Set[Player](BluePlayer)
    val serverPlayers = Set[Player](OrangePlayer)
    val connection = new LocalConnection(Set(0))
    val clientConnection0 = new LocalClientConnection(0, connection, clientPlayers)
    val serverConnection = new LocalServerConnection(0, connection)
    val server = new DroneWorldSimulator(
      TheGameMaster.defaultMap.createGameConfig(
        droneControllers = Seq(new DummyDroneController, TheGameMaster.level1AI()),
        tickPeriod = 10
      ),
      t => Seq.empty,
      AuthoritativeServerConfig(serverPlayers,
                                clientPlayers,
                                Set(clientConnection0),
                                s => (),
                                s => (),
                                30.seconds)
    )
    val client = new DroneWorldSimulator(
      TheGameMaster.defaultMap.createGameConfig(
        droneControllers = Seq(TheGameMaster.level2AI(), new DummyDroneController),
        tickPeriod = 10
      ),
      t => Seq.empty,
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
