package cwinter.codecraft.core.multiplayer

import java.nio.ByteBuffer
import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.core.objects.drone._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps



private[core] class WebsocketServerConnection(
  val connection: WebsocketClient,
  val debug: Boolean = false
) extends RemoteServer {

  private var _gameClosed = Option.empty[GameClosed.Reason]

  val initialServerResponse = Promise[InitialServerResponse]

  private[this] var serverCommands = Promise[Either[Seq[(Int, SerializableDroneCommand)], GameClosed.Reason]]
  private[this] var worldState = Promise[Either[WorldStateMessage, GameClosed.Reason]]
  private[this] var nanoTimeLastResponse = System.nanoTime()


  connection.registerOnMessage(handleMessage)

  def register(): Unit = sendMessage(Register)

  def handleMessage(client: WebsocketClient, message: ByteBuffer): Unit = synchronized {
    if (debug) println(message)
    nanoTimeLastResponse = System.nanoTime()
    ServerMessage.parseBytes(message) match {
      case CommandsMessage(commands) => serverCommands.success(Left(commands))
      case state: WorldStateMessage => worldState.success(Left(state))
      case start: InitialSync => initialServerResponse.success(start)
      case ServerBusy => initialServerResponse.success(ServerBusy)
      case rtt: RTT => sendMessage(rtt)
      case GameClosed(reason) =>
        _gameClosed = Some(reason)
        if (!serverCommands.isCompleted) serverCommands.success(Right(reason))
        if (!worldState.isCompleted) worldState.success(Right(reason))
        serverCommands = Promise.successful(Right(reason))
        worldState = Promise.successful(Right(reason))
    }
  }

  def receiveInitialWorldState(): Future[InitialServerResponse] = initialServerResponse.future

  override def receiveCommands()(implicit context: SimulationContext): Result[Seq[(Int, DroneCommand)]] = synchronized {
    if (debug) println(s"[t=${context.timestep}] Waiting for commands...")
    for (reply <- serverCommands.future) yield {
      reply match {
        case Left(commands) =>
          if (debug) println ("Commands received.")
          serverCommands = Promise[Either[Seq[(Int, SerializableDroneCommand)], GameClosed.Reason]]
          Left(deserialize(commands))
        case Right(reason) => Right(reason)
      }
    }
  }

  override def receiveWorldState(): Result[WorldStateMessage] = synchronized {
    for (state <- worldState.future) yield {
      worldState = Promise[Either[WorldStateMessage, GameClosed.Reason]]
      state
    }
  }

  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    sendMessage(CommandsMessage(serializable))
    if (debug) println(s"sendCommands($commands)")
  }

  def deserialize(commands: Seq[(Int, SerializableDroneCommand)])(
    implicit context: SimulationContext
  ): Seq[(Int, DroneCommand)] =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))

  def sendMessage(m: ClientMessage): Unit = connection.sendMessage(ClientMessage.serializeBinary(m))

  def gameClosed = _gameClosed

  override def msSinceLastResponse: Int = ((System.nanoTime() - nanoTimeLastResponse) / 1000000).toInt
}

