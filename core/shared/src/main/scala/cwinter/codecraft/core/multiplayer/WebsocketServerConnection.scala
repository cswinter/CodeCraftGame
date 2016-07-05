package cwinter.codecraft.core.multiplayer

import java.nio.ByteBuffer

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.objects.drone._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps



private[core] class WebsocketServerConnection(
  val connection: WebsocketClient,
  val debug: Boolean = false
) extends RemoteServer {

  val initialWorldState = Promise[InitialSync]

  private[this] var serverCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
  private[this] var worldState = Promise[WorldStateMessage]


  connection.onMessage(handleMessage)
  connection.sendMessage(Register.toBinary)


  def handleMessage(client: WebsocketClient, message: ByteBuffer): Unit = {
    if (debug) println(message)
    MultiplayerMessage.parseBytes(message) match {
      case CommandsMessage(commands) => serverCommands.success(commands)
      case state: WorldStateMessage => worldState.success(state)
      case start: InitialSync => initialWorldState.success(start)
      case Register =>
      case rtt: RTT => connection.sendMessage(rtt.toBinary)
    }
  }

  def receiveInitialWorldState(): Future[InitialSync] =
    initialWorldState.future

  override def receiveCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]] = {
    if (debug) println(s"[t=${context.timestep}] Waiting for commands...")
    for (commands <- serverCommands.future) yield {
      if (debug) println("Commands received.")
      serverCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
      deserialize(commands)
    }
  }

  override def receiveWorldState(): Future[WorldStateMessage] = {
    for (state <- worldState.future) yield {
      worldState = Promise[WorldStateMessage]
      state
    }
  }

  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    val message = CommandsMessage(serializable).toBinary
    connection.sendMessage(message)
    if (debug) println(s"sendCommands($commands)")
  }


  def deserialize(commands: Seq[(Int, SerializableDroneCommand)])(
    implicit context: SimulationContext
  ): Seq[(Int, DroneCommand)] =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))
}

