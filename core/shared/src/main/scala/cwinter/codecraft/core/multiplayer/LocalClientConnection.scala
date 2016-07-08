package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.game.SimulationContext
import cwinter.codecraft.core.objects.drone._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps



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
  override def waitForCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]] = {
    for (commands <- connection.popClientCommands(clientID)) yield {
      connection.resetState()
      commands
    }
  }

  override def sendWorldState(worldState: WorldStateMessage): Unit = {
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
  override def receiveCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]] = {
    connection.getCommandsForClient(clientID)
  }

  override def receiveWorldState(): Future[WorldStateMessage] = {
    connection.getWorldState()
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
  var state = Promise[WorldStateMessage]
  resetCommandsForClients()
  resetCommandsFromClients()

  def resetCommandsForClients(): Unit = promisedCommandsForClient = {
      for (id <- clientIDs) yield (id, Promise[SerializedCommands])
  }.toMap

  def resetCommandsFromClients(): Unit = promisedCommandsFromClients = {
    for (id <- clientIDs) yield (id, Promise[SerializedCommands])
  }.toMap

  def resetState(): Unit = state = Promise[WorldStateMessage]

  def putClientCommands(clientID: Int, commands: Commands): Unit = this.synchronized {
    promisedCommandsFromClients(clientID).success(serialize(commands))
  }

  def popClientCommands(clientID: Int)(
    implicit context: SimulationContext
  ): Future[Commands] = this.synchronized {
    promisedCommandsFromClients(clientID).future.map(deserialize)
  }

  def putWorldState(state: WorldStateMessage): Unit = this.synchronized {
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

  def getWorldState(): Future[WorldStateMessage] = state.future

  def serialize(commands: Commands): SerializedCommands =
    for ((id, command) <- commands) yield (id, command.toSerializable)

  def deserialize(commands: SerializedCommands)(
    implicit context: SimulationContext
  ): Commands =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))
}

