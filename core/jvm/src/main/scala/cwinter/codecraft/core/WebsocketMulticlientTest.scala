package cwinter.codecraft.core

import cwinter.codecraft.core.api.TheGameMaster

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

private[core] object WebsocketMulticlientTest {
  def main(args: Array[String]): Unit = {
    new Thread {
      override def run(): Unit = {
        multiplayer.Server.spawnServerInstance2()
      }
    }.start()

    Thread.sleep(2000, 0)

    async {
      val client1 = await {
        TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.level2AI())
      }
      client1.run()
      Thread.sleep(35000, 0)
      val client2 = await {
        TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.level2AI())
      }
      val client3 = await {
        TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.level2AI())
      }
      client2.run()
      client3.run()
    }
  }
}
