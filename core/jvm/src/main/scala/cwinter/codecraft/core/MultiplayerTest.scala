package cwinter.codecraft.core

import cwinter.codecraft.core.api.TheGameMaster

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global


private[core] object MultiplayerTest {
  def main(args: Array[String]): Unit = async {
    val client = await { TheGameMaster.prepareMultiplayerGame("192.168.2.113", TheGameMaster.level2AI()) }
    TheGameMaster.run(client)
  }
}

