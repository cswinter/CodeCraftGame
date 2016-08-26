package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.api.DroneControllerBase
import cwinter.codecraft.core.game.{DroneWorldSimulator, MultiplayerClientConfig}
import cwinter.codecraft.core.objects.drone.InitialSync

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class ServerConnection(
  val serverAddress: String,
  val onStateTransition: ConnectionState => Unit = _ => Unit
) {
  private[this] var _state: ConnectionState = Connecting

  def connect(): Unit =
    try {
      val websocket: WebsocketClient = CrossPlatformWebsocket.create(s"ws://$serverAddress:8080")
      websocket.registerOnOpen(upgrade)
      websocket.connect()
    } catch {
      case x: Throwable => state = Error(x)
    }

  private def upgrade(ws: WebsocketClient): Unit = {
    state = WaitingForPlayer
    async[DroneControllerBase => DroneWorldSimulator] {
      val serverConnection = new WebsocketServerConnection(ws)
      val sync = await { serverConnection.receiveInitialWorldState() }

      createGame(sync, serverConnection)
    }.onComplete {
      case Success(connection) => state = FoundGame(connection)
      case Failure(x) => state = Error(x)
    }
  }

  private def createGame(
    sync: InitialSync,
    conn: WebsocketServerConnection
  )(controller: DroneControllerBase): DroneWorldSimulator = {
    val clientPlayers = sync.localPlayers
    val serverPlayers = sync.remotePlayers
    val gameConfig = sync.gameConfig(Seq(controller))
    val mpConfig = MultiplayerClientConfig(clientPlayers, serverPlayers, conn)
    assert(gameConfig.drones.count { case (d, _) => mpConfig.isLocalPlayer(d.player) } == 1,
           "Must have one drone owned by local player.")
    new DroneWorldSimulator(
      config = gameConfig,
      multiplayerConfig = mpConfig
    )
  }

  private def state_=(value: ConnectionState): Unit = synchronized {
    _state = value
    onStateTransition(value)
  }
  def state: ConnectionState = _state
}

sealed trait ConnectionState

case class Error(x: Throwable) extends ConnectionState
case object Connecting extends ConnectionState
case object WaitingForPlayer extends ConnectionState
case class FoundGame(connection: DroneControllerBase => DroneWorldSimulator) extends ConnectionState
case class Completed(gameResult: Any)
