package cwinter.codecraft.core.multiplayer

import java.nio.ByteBuffer

import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player}
import cwinter.codecraft.core.game.{SimulationContext, WorldMap}
import cwinter.codecraft.core.objects.drone.GameClosed.Reason
import cwinter.codecraft.core.objects.drone._
import cwinter.codecraft.util.AggregateStatistics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}


private[core] class WebsocketClientConnection(
  override val players: Set[Player],
  val map: WorldMap,
  val rngSeed: Int,
  val debug: Boolean = false,
  val info: Boolean = false
) extends RemoteClient with WebsocketWorker {
  private var clientCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
  private val commandsSentSize = new AggregateStatistics
  private val commandsReceivedSize = new AggregateStatistics
  private val positionsSentSize = new AggregateStatistics


  override def receive(message: String): Unit = {
    if (debug) println(message)
    try {
      handleMessage(MultiplayerMessage.parse(message))
    } catch {
      case t: Throwable =>
        println(s"Failed to deserialize string '$message'")
        println(s"Exception message: ${t.getMessage}")
    }
  }

  override def receiveBytes(message: ByteBuffer): Unit = {
    try {
      commandsReceivedSize.addMeasurement(message.remaining())
      val msg = MultiplayerMessage.parseBytes(message)
      handleMessage(msg)
    } catch {
      case t: Throwable =>
        println(s"Failed to deserialize bytes '$message'")
        println(s"Exception: $t")
        t.printStackTrace()
    }
  }

  private def handleMessage(msg: MultiplayerMessage): Unit = msg match {
    case CommandsMessage(commands) => clientCommands.success(commands)
    case Register => send(syncMessage)
    case RTT(time, message) => if (debug) {
      val ms = (System.nanoTime - time) / 1000000.0
      println(f"RTT for $message: $ms%.2fms")
    }
    case _: WorldStateMessage => protocolViolation("Authoritative server received WorldStateMessage!")
    case _: InitialSync => protocolViolation("Authoritative server received InitialSync!")
    case _: GameClosed => protocolViolation("Client attempted to close game.")
  }

  private def protocolViolation(message: String): Nothing = {
    throw new Exception(message)
  }

  def syncMessage =
    InitialSync(
      map.size,
      map.minerals,
      map.initialDrones.map(x => SerializableSpawn(x)),
      players.map(_.id),
      (Set(OrangePlayer, BluePlayer) -- players).map(_.id),
      rngSeed,
      map.winConditions
    ).toBinary

  override def waitForCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]] = {
    if (debug) println(s"[t=${context.timestep}] Waiting for commands...")
    for (commands <- clientCommands.future) yield {
      if (debug) println("Commands received.")
      clientCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
      deserialize(commands)
    }
  }

  override def sendWorldState(worldState: WorldStateMessage): Unit = {
    val serialized = MultiplayerMessage.serializeBinary(worldState)
    positionsSentSize.addMeasurement(serialized.remaining)
    send(serialized)
    if (info && positionsSentSize.count > 0 && positionsSentSize.count % 1000 == 0) {
      println(s"Positions Message Size: ${positionsSentSize.display}")
      println(s"Commands Sent Message Size: ${commandsSentSize.display}")
      println(s"Commands Received Message Size: ${commandsReceivedSize.display}")
      val bandwidthEstimate =
        (commandsReceivedSize.ema + commandsSentSize.ema + positionsSentSize.ema) * 8 * 60 / 1000
      println(f"Required bandwidth for 60FPS: $bandwidthEstimate%.3gKbps")
    }
  }

  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    val serialized = CommandsMessage(serializable).toBinary
    commandsSentSize.addMeasurement(serialized.remaining)
    send(serialized)
  }

  def deserialize(commands: Seq[(Int, SerializableDroneCommand)])(
    implicit context: SimulationContext
  ): Seq[(Int, DroneCommand)] =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))


  override def close(reason: Reason): Unit = {
    send(GameClosed(reason).toBinary)
    closeConnection()
  }
}

