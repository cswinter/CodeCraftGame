package cwinter.codecraft.core.multiplayer

import akka.actor.{ActorRef, Props}
import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.network.RemoteClient
import cwinter.codecraft.core.objects.drone.{DroneStateMessage, DroneCommand}
import cwinter.codecraft.core.replay.DummyDroneController
import cwinter.codecraft.core.{SimulationContext, AuthoritativeServerConfig, DroneWorldSimulator}
import spray.can.websocket
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{BinaryFrame, TextFrame}
import spray.http.HttpRequest
import upickle.default._


private[core] class MultiplayerGameWorker(val serverConnection: ActorRef)
extends websocket.WebSocketServerWorker with RemoteClient {
  val clientPlayers = Set[Player](BluePlayer)
  val serverPlayers = Set[Player](OrangePlayer)
  val map = TheGameMaster.defaultMap()
  val server = new DroneWorldSimulator(
    map,
    Seq(new DummyDroneController, TheGameMaster.level2AI()),
    t => Seq.empty,
    None,
    AuthoritativeServerConfig(serverPlayers, clientPlayers, Set(this))
  )
  server.run()

  override def receive = handshaking orElse closeLogic

  def businessLogic: Receive = {
    case BinaryFrame(bytes) => throw new Exception("Received unexpected binary frame")
    case TextFrame(text) =>
      // TODO: parse text
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case x: HttpRequest => // do something
  }

  override def waitForCommands()(implicit context: SimulationContext): Seq[(Int, DroneCommand)] = {
    Seq.empty[(Int, DroneCommand)]
  }
  override def players: Set[Player] = clientPlayers
  override def sendWorldState(worldState: Iterable[DroneStateMessage]): Unit = {
    send(TextFrame(write(worldState)))
  }
  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    val serialized = write(serializable)
    send(TextFrame(serialized))
  }
}


object MultiplayerGameWorker {
  def props(serverConnection: ActorRef) = Props(classOf[MultiplayerGameWorker], serverConnection)
}

