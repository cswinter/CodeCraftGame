package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.objects.drone._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._



// ** FULL MULTIPLAYER PROTOCOL **
// [CLIENTS + SERVER] COLLECT COMMANDS FROM LOCAL DRONES
// [CLIENTS] SEND COMMANDS FROM LOCAL DRONES
// [SERVER] RECEIVE ALL COMMANDS
// [SERVER] DISTRIBUTE COMMANDS TO ALL CLIENTS
// [CLIENTS] RECEIVE COMMANDS FROM SERVER
// [CLIENTS + SERVER] EXECUTE COMMANDS FROM REMOTE DRONES
// [SERVER] COLLECT WORLD STATE, SEND TO CLIENTS
// [CLIENTS] RECEIVE AND APPLY WORLD STATE

private[core] class LocalClientConnection(
  clientID: Int,
  connection: LocalConnection,
  override val players: Set[Player]
) extends RemoteClient {
  override def waitForCommands()(implicit context: SimulationContext): Seq[(Int, DroneCommand)] = {
    val result = Await.result(connection.popClientCommands(clientID), 5 seconds)
    connection.resetState()
    result
  }

  override def sendWorldState(worldState: Iterable[DroneStateMessage]): Unit = {
    connection.resetCommandsForClients()
    connection.putWorldState(worldState)
  }

  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    connection.resetCommandsFromClients()
    connection.putCommandsForClient(clientID, commands)
  }
}

private[core] class LocalServerConnection(
  clientID: Int,
  connection: LocalConnection
) extends RemoteServer {
  override def receiveCommands()(implicit context: SimulationContext): Seq[(Int, DroneCommand)] = {
    Await.result(connection.getCommandsForClient(clientID), 5 seconds)
  }

  override def receiveWorldState(): Iterable[DroneStateMessage] = {
    val result = Await.result(connection.getWorldState(), 5 seconds)
    result
  }

  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    connection.putClientCommands(clientID, commands)
  }
}


private[core] class LocalConnection(
  clientIDs: Set[Int]
) {
  type Commands = Seq[(Int, DroneCommand)]
  type SerializedCommands = Seq[(Int, SerializableDroneCommand)]
  var promisedCommandsForClient = Map.empty[Int, Promise[SerializedCommands]]
  var promisedCommandsFromClients = Map.empty[Int, Promise[SerializedCommands]]
  var state = Promise[Iterable[DroneStateMessage]]
  resetCommandsForClients()
  resetCommandsFromClients()

  def resetCommandsForClients(): Unit = promisedCommandsForClient = {
      for (id <- clientIDs) yield (id, Promise[SerializedCommands])
  }.toMap

  def resetCommandsFromClients(): Unit = promisedCommandsFromClients = {
    for (id <- clientIDs) yield (id, Promise[SerializedCommands])
  }.toMap

  def resetState(): Unit = state = Promise[Iterable[DroneStateMessage]]

  def putClientCommands(clientID: Int, commands: Commands): Unit = this.synchronized {
    promisedCommandsFromClients(clientID).success(serialize(commands))
  }

  def popClientCommands(clientID: Int)(
    implicit context: SimulationContext
  ): Future[Commands] = this.synchronized {
    promisedCommandsFromClients(clientID).future.map(deserialize)
  }

  def putWorldState(state: Iterable[DroneStateMessage]): Unit = this.synchronized {
    if (!this.state.isCompleted) {
      this.state.success(state)
    }
  }

  def putCommandsForClient(clientID: Int, commands: Commands): Unit = this.synchronized {
    promisedCommandsForClient(clientID).success(serialize(commands))
  }

  def getCommandsForClient(clientID: Int)(
    implicit context: SimulationContext
  ): Future[Seq[(Int, DroneCommand)]] = this.synchronized {
    promisedCommandsForClient(clientID).future.map(deserialize)
  }

  def getWorldState(): Future[Iterable[DroneStateMessage]] = state.future

  def serialize(commands: Commands): SerializedCommands =
    for ((id, command) <- commands) yield (id, command.toSerializable)

  def deserialize(commands: SerializedCommands)(
    implicit context: SimulationContext
  ): Commands =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))
}

