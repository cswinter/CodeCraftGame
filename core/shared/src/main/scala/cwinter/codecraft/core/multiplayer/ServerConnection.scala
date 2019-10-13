package cwinter.codecraft.core.multiplayer

import java.util.UUID

import cwinter.codecraft.core.api.DroneControllerBase
import cwinter.codecraft.core.game.{DroneWorldSimulator, MultiplayerClientConfig}
import cwinter.codecraft.core.objects.drone.{ServerBusy, InitialSync}

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class ServerConnection(
  val serverAddress: String,
  val onStateTransition: ConnectionState => Unit = _ => Unit
) {
  private[this] var _state: ConnectionState = Connecting
  private[this] var messageHandler = Option.empty[WebsocketServerConnection]
  val uuid = UUID.randomUUID()

  def connect(): Unit =
    try {
      val websocket: WebsocketClient = CrossPlatformWebsocket.create(s"ws://$serverAddress:8080")
      messageHandler = Some(new WebsocketServerConnection(websocket))
      websocket.registerOnOpen(upgrade)
      websocket.connect()
    } catch {
      case x: Throwable => state = Error(x)
    }

  private def upgrade(ws: WebsocketClient): Unit = {
    state = WaitingForPlayer
    async[DroneControllerBase => DroneWorldSimulator] {
      messageHandler.get.register()
      val sync = await { messageHandler.get.receiveInitialWorldState() }

      sync match {
        case sync: InitialSync => createGame(sync, messageHandler.get)
        case ServerBusy =>
          throw new Exception(
            "The server is not accepting additional connections right now. Try again later.")
      }
    }.onComplete {
      case Success(connection) => state = FoundGame(connection)
      case Failure(x) => state = Error(x)
    }
  }

  private def createGame(
    sync: InitialSync,
    conn: WebsocketServerConnection
  )(controller: DroneControllerBase): DroneWorldSimulator = {
    println(f"Received initial sync $sync")
    val clientPlayers = sync.localPlayers
    val serverPlayers = sync.remotePlayers
    val gameConfig = sync.gameConfig(Seq(controller))
    val mpConfig = MultiplayerClientConfig(clientPlayers, serverPlayers, conn)
    // assert(gameConfig.drones.count { case (d, _) => mpConfig.isLocalPlayer(d.player) } == 1,
    //       "Must have one drone owned by local player.")
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
