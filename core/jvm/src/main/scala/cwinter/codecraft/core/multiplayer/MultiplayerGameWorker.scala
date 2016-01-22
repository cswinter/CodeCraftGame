package cwinter.codecraft.core.multiplayer

import akka.actor.{ActorRef, Props}
import cwinter.codecraft.core._
import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.core.replay.DummyDroneController
import spray.can.websocket
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{BinaryFrame, TextFrame}
import spray.http.HttpRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Await, Promise}
import scala.language.postfixOps



private[core] class MultiplayerGameWorker(val serverConnection: ActorRef, displayGame: Boolean = false)
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

  if (displayGame) {
    TheGameMaster.run(server)
  } else {
    server.run()
  }

  private[this] var clientCommands = Promise[Seq[(Int, SerializableDroneCommand)]]

  override def receive = handshaking orElse closeLogic

  def businessLogic: Receive = {
    case BinaryFrame(bytes) => throw new Exception("Received unexpected binary frame")
    case TextFrame(text) =>
      val decoded = text.decodeString("UTF-8")
      println(decoded)
      try {
        MultiplayerMessage.parse(decoded) match {
          case CommandsMessage(commands, _) =>
            clientCommands.success(commands)
          case WorldStateMessage(_) =>
            throw new Exception("Authoritative server received WorldStateMessage!")
          case _: InitialSync =>
            throw new Exception("Authoritative server received InitialSync!")
          case Register =>
            send(TextFrame(MultiplayerMessage.serialize(map.size, map.minerals, map.initialDrones)))
        }
      } catch {
        case t: Throwable =>
          println(s"Failed to deserialize string '$decoded'")
          println(s"Exception message: ${t.getMessage}")
      }
    case x: FrameCommandFailed =>
      log.error("frame command failed", x)
      throw new Exception(s"Frame command failed: $x")
    case x: HttpRequest =>
      throw new Exception("Unexpected HttpRequest")
  }

  override def waitForCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]] = {
    println(s"[t=${context.timestep}] Waiting for commands...")
    for (
      commands <- clientCommands.future
    ) yield {
      println("Commands received.")
      clientCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
      deserialize(commands)
    }
  }

  override def players: Set[Player] = clientPlayers
  override def sendWorldState(worldState: Iterable[DroneStateMessage]): Unit = {
    send(TextFrame(MultiplayerMessage.serialize(worldState)))
  }
  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    val serialized = MultiplayerMessage.serialize(serializable)
    send(TextFrame(serialized))
  }

  def deserialize(commands: Seq[(Int, SerializableDroneCommand)])(
    implicit context: SimulationContext
  ): Seq[(Int, DroneCommand)] =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))
}


object MultiplayerGameWorker {
  def props(serverConnection: ActorRef, displayGame: Boolean = false) =
    Props(classOf[MultiplayerGameWorker], serverConnection, displayGame)
}

