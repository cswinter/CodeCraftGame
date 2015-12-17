package cwinter.codecraft.core.network

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.objects.drone.{DroneCommand, DroneDynamicsState}

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
  override def waitForCommands(): Seq[(Int, DroneCommand)] = {
    connection.resetState()
    Await.result(connection.popClientCommands(clientID), 1 seconds)
  }

  override def sendWorldState(worldState: Iterable[DroneDynamicsState]): Unit = {
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
  override def receiveCommands(): Seq[(Int, DroneCommand)] =
    Await.result(connection.getCommandsForClient(clientID), 1 seconds)

  override def receiveWorldState(): Iterable[DroneDynamicsState] =
    Await.result(connection.getWorldState(), 1 seconds)

  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit =
    connection.putClientCommands(clientID, commands)
}


private[core] class LocalConnection(
  clientIDs: Set[Int]
) {
  type Commands = Seq[(Int, DroneCommand)]
  var promisedCommandsForClient = Map.empty[Int, Promise[Commands]]
  var promisedCommandsFromClients = Map.empty[Int, Promise[Commands]]
  var state = Promise[Iterable[DroneDynamicsState]]
  resetCommandsForClients()
  resetCommandsFromClients()

  def resetCommandsForClients(): Unit = promisedCommandsForClient = {
      for (id <- clientIDs) yield (id, Promise[Commands])
  }.toMap

  def resetCommandsFromClients(): Unit = promisedCommandsFromClients = {
    for (id <- clientIDs) yield (id, Promise[Commands])
  }.toMap

  def resetState(): Unit = state = Promise[Iterable[DroneDynamicsState]]

  def putClientCommands(clientID: Int, commands: Commands): Unit = this.synchronized {
    promisedCommandsFromClients(clientID).success(commands)
  }

  def popClientCommands(clientID: Int): Future[Commands] = this.synchronized {
    promisedCommandsFromClients(clientID).future
  }

  def putWorldState(state: Iterable[DroneDynamicsState]): Unit = this.synchronized {
    this.state.success(state)
  }

  def putCommandsForClient(clientID: Int, commands: Commands): Unit = this.synchronized {
    promisedCommandsForClient(clientID).success(commands)
  }

  def getCommandsForClient(clientID: Int): Future[Seq[(Int, DroneCommand)]] = this.synchronized {
    promisedCommandsForClient(clientID).future
  }

  def getWorldState(): Future[Iterable[DroneDynamicsState]] = state.future
}

