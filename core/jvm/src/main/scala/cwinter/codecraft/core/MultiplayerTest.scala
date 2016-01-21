package cwinter.codecraft.core

import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.multiplayer.WebsocketServerConnection
import cwinter.codecraft.core.replay.DummyDroneController


object MultiplayerTest {
  def main(args: Array[String]): Unit = {

    val client = TheGameMaster.prepareMultiplayerGame("192.168.2.113", TheGameMaster.level2AI())

    TheGameMaster.run(client)
  }
}

