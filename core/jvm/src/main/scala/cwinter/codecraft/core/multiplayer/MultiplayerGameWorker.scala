package cwinter.codecraft.core.multiplayer

import akka.actor.{ActorRef, Props}
import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.network.RemoteClient
import cwinter.codecraft.core.objects.drone.{DroneCommand, DroneStateMessage, SerializableDroneCommand}
import cwinter.codecraft.core.replay.DummyDroneController
import cwinter.codecraft.core.{AuthoritativeServerConfig, DroneWorldSimulator, SimulationContext}
import spray.can.websocket
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{BinaryFrame, TextFrame}
import spray.http.HttpRequest
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.language.postfixOps


sealed trait MultiplayerMessage
@key("Cmds") case class CommandsMessage(commands: Seq[(Int, SerializableDroneCommand)]) extends MultiplayerMessage

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
  TheGameMaster.run(server)

  private[this] var clientCommands = Promise[Seq[(Int, SerializableDroneCommand)]]

  override def receive = handshaking orElse closeLogic

  def businessLogic: Receive = {
    case BinaryFrame(bytes) => throw new Exception("Received unexpected binary frame")
    case TextFrame(text) =>
      val decoded = text.decodeString("UTF-8")
      println(decoded)
      try {
        read[MultiplayerMessage](decoded) match {
          case CommandsMessage(commands) =>
            clientCommands.success(commands)
        }
      } catch {
        case t: Throwable =>
          println(s"Failed to deserialize string '$decoded'")
          println(s"Exception message: ${t.getMessage}")
      }
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
    case x: HttpRequest => // do something
  }

  override def waitForCommands()(implicit context: SimulationContext): Seq[(Int, DroneCommand)] = {
    val result = Await.result(clientCommands.future.map(deserialize), 30 seconds)
    clientCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
    result
  }
  override def players: Set[Player] = clientPlayers
  override def sendWorldState(worldState: Iterable[DroneStateMessage]): Unit = {
    send(TextFrame(write(worldState)))
  }
  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    val serialized = write(CommandsMessage(serializable))
    send(TextFrame(serialized))
  }

  def deserialize(commands: Seq[(Int, SerializableDroneCommand)])(
    implicit context: SimulationContext
  ): Seq[(Int, DroneCommand)] =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))
}


object MultiplayerGameWorker {
  def props(serverConnection: ActorRef) = Props(classOf[MultiplayerGameWorker], serverConnection)
}
