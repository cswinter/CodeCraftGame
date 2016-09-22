package cwinter.codecraft.core

import cwinter.codecraft.core.api.TheGameMaster

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

private[core] object WebsocketMultiplayerTest {
  def main(args: Array[String]): Unit = {
    new Thread {
      override def run(): Unit = {
        multiplayer.Server.spawnServerInstance2()
      }
    }.start()

    Thread.sleep(2000, 0)

    val client1Fut = TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.replicatorAI())
    val client2Fut = TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.replicatorAI())
    for {
      client1 <- client1Fut
      client2 <- client2Fut
    } {
      client2.framerateTarget = 1001
      client2.run()
      TheGameMaster.run(client1)
    }
  }
}
