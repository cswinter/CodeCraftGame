package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.objects.drone._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Await, Promise}
import scala.language.postfixOps



private[core] class WebsocketServerConnection(
  val connection: WebsocketClient
) extends RemoteServer {

  val initialWorldState = Promise[InitialSync]

  private[this] var serverCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
  private[this] var worldState = Promise[Iterable[DroneStateMessage]]


  connection.onMessage(handleMessage)
  connection.sendMessage(MultiplayerMessage.register)


  def handleMessage(client: WebsocketClient, message: String): Unit = {
    println(message)
    MultiplayerMessage.parse(message) match {
      case CommandsMessage(commands, _) =>
        serverCommands.success(commands)
      case WorldStateMessage(state) =>
        worldState.success(state)
      case start: InitialSync =>
        initialWorldState.success(start)
      case Register =>
    }
  }

  def receiveInitialWorldState(): InitialSync =
    Await.result(initialWorldState.future, 30 seconds)

  override def receiveCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]] = {
    println(s"[t=${context.timestep}] Waiting for commands...")
    for (commands <- serverCommands.future) yield {
      println("Commands received.")
      serverCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
      deserialize(commands)
    }
  }

  override def receiveWorldState(): Future[Iterable[DroneStateMessage]] = {
    for (state <- worldState.future) yield {
      worldState = Promise[Iterable[DroneStateMessage]]
      state
    }
  }

  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    val message = MultiplayerMessage.serialize(serializable)
    println(s"sendCommands($message)")
    connection.sendMessage(message)
  }


  def deserialize(commands: Seq[(Int, SerializableDroneCommand)])(
    implicit context: SimulationContext
  ): Seq[(Int, DroneCommand)] =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))
}

