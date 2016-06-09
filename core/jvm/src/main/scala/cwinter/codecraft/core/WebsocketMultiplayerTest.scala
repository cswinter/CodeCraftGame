package cwinter.codecraft.core

import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.core.objects.drone.{SerializableMoveToMineralCrystal, MultiplayerMessage}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

private[core] object WebsocketMultiplayerTest {
  def main(args: Array[String]): Unit = {
    /*
    import boopickle.Default._
    /*implicit val mpMsgPickler = compositePickler[MultiplayerMessage]
        .addConcreteType[CommandsMessage]
        .addConcreteType[InitialSync]
        .addConcreteType[Register.type]*/

    val msg = MultiplayerMessage.serializeBinary(Seq((0, SerializableMoveToMineralCrystal(0))))
    println(msg)
    val decoded = MultiplayerMessage.parseBytes(msg)
    println(decoded)
    //val msg = MultiplayerMessage.serializeBinary()
    //val decoded = MultiplayerMessage.parseBytes(msg)

    return*/

    new Thread {
      override def run(): Unit = {
        multiplayer.Server.spawnServerInstance()
      }
    }.start()

    Thread.sleep(2000, 0)

    async {
      val client = await {
        TheGameMaster.prepareMultiplayerGame("localhost", TheGameMaster.replicatorAI())
      }
      TheGameMaster.run(client)
    }
  }
}
