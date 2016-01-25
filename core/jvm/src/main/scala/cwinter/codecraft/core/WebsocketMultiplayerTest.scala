package cwinter.codecraft.core

import cwinter.codecraft.core.api.TheGameMaster

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

object WebsocketMultiplayerTest {
  def main(args: Array[String]): Unit = {
    new Thread {
      override def run(): Unit = {
        multiplayer.Server.spawnServerInstance()
      }
    }.start()

    Thread.sleep(2000, 0)

    async {
      val client = await {
        TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.level2AI())
      }
      TheGameMaster.run(client)
    }
  }
}
