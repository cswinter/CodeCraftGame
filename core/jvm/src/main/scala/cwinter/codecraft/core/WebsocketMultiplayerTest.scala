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

    val client = TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.level2AI())
    TheGameMaster.run(client)
  }
}
