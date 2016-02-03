package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.api.{OrangePlayer, BluePlayer, Player}
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.core.{SimulationContext, WorldMap}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}


private[core] class RemoteWebsocketClient(
  override val players: Set[Player],
  val map: WorldMap,
  val debug: Boolean = false
) extends RemoteClient with WebsocketWorker {
  private[this] var clientCommands = Promise[Seq[(Int, SerializableDroneCommand)]]


  override def receive(message: String): Unit = {
    if (debug)
      println(message)
    try {
      MultiplayerMessage.parse(message) match {
        case CommandsMessage(commands) =>
          clientCommands.success(commands)
        case WorldStateMessage(_) =>
          throw new Exception("Authoritative server received WorldStateMessage!")
        case _: InitialSync =>
          throw new Exception("Authoritative server received InitialSync!")
        case Register =>
          send(syncMessage)
      }
    } catch {
      case t: Throwable =>
        println(s"Failed to deserialize string '$message'")
        println(s"Exception message: ${t.getMessage}")
    }
  }

  def syncMessage =
    MultiplayerMessage.serialize(
      map.size,
      map.minerals,
      map.initialDrones,
      players,
      Set(OrangePlayer, BluePlayer) -- players
    )

  override def waitForCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]] = {
    if (debug)
      println(s"[t=${context.timestep}] Waiting for commands...")
    for (
      commands <- clientCommands.future
    ) yield {
      if (debug)
        println("Commands received.")
      clientCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
      deserialize(commands)
    }
  }

  override def sendWorldState(worldState: Iterable[DroneStateMessage]): Unit = {
    send(MultiplayerMessage.serialize(worldState))
  }

  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    val serialized = MultiplayerMessage.serialize(serializable)
    send(serialized)
  }

  def deserialize(commands: Seq[(Int, SerializableDroneCommand)])(
    implicit context: SimulationContext
  ): Seq[(Int, DroneCommand)] =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))
}
