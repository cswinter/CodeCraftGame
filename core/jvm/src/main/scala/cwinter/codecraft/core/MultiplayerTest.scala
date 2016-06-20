package cwinter.codecraft.core

import cwinter.codecraft.core.api.TheGameMaster

import scala.concurrent.Await
import scala.concurrent.duration._


private[core] object MultiplayerTest {
  def main(args: Array[String]): Unit = {
    val client = Await.result(TheGameMaster.prepareMultiplayerGame("192.168.2.107", TheGameMaster.replicatorAI()), 10.seconds)
    TheGameMaster.run(client)
  }
}

