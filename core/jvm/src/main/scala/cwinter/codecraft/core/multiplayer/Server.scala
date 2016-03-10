package cwinter.codecraft.core.multiplayer

import akka.actor._
import akka.io.IO
import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.replay.DummyDroneController
import cwinter.codecraft.core.{AuthoritativeServerConfig, DroneWorldSimulator}
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object Server {
  def spawnServerInstance(displayGame: Boolean = false): Unit = {
    implicit val system = ActorSystem()
    val server = system.actorOf(MultiplayerServer.props(displayGame), "websocket")
    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)
    system.awaitTermination()
  }

  def spawnServerInstance2(): Unit = {
    implicit val system = ActorSystem()
    val server = system.actorOf(Props(classOf[TwoPlayerMultiplayerServer]), "websocket")
    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)
    system.awaitTermination()
  }

  def start(): ActorRef = {
    implicit val system = ActorSystem()
    val server = system.actorOf(Props(classOf[TwoPlayerMultiplayerServer]), "websocket")
    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)
    server
  }

  def main(args: Array[String]): Unit = {
    spawnServerInstance(false)
  }


  object Stop
  object GetStatus
  case class Status(
    connections: Int,
    runningGames: Int,
    freeSlots: Int
  )
}


private[codecraft] class MultiplayerServer(displayGame: Boolean = false) extends Actor with ActorLogging {
  val map = TheGameMaster.defaultMap
  val clientPlayers = Set[Player](BluePlayer)
  val serverPlayers = Set[Player](OrangePlayer)


  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val worker = new RemoteWebsocketClient(clientPlayers, map)
      val conn = context.actorOf(WebsocketActor.props(serverConnection, worker))

      val server = new DroneWorldSimulator(
        map,
        Seq(new DummyDroneController, TheGameMaster.level2AI()),
        t => Seq.empty,
        None,
        AuthoritativeServerConfig(serverPlayers, clientPlayers, Set(worker))
      )

      serverConnection ! Http.Register(conn)

      if (displayGame) {
        TheGameMaster.run(server)
      } else {
        server.run()
      }
  }
}

private[codecraft] class TwoPlayerMultiplayerServer extends Actor with ActorLogging {
  import Server.{Stop, GetStatus, Status}
  val map = TheGameMaster.defaultMap

  private[this] var clients = Set.empty[RemoteClient]
  private[this] var freeSlots = Seq(MultiplayerSlot(OrangePlayer), MultiplayerSlot(BluePlayer))
  private[this] var slotAssignments = Map.empty[ActorRef, Connection]
  private[this] var runningGame: Option[DroneWorldSimulator] = None

  case class Connection(
    rawConnection: ActorRef,
    websocketActor: ActorRef,
    worker: RemoteWebsocketClient,
    assignedSlot: MultiplayerSlot
  )

  case class GameTimedOut(simulator: DroneWorldSimulator)


  def receive = {
    case m@Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      if (hasFreeSlot) {
         acceptConnection(serverConnection)
        if (freeSlotCount == 0) startGame()
      } else {
        // Connection will automatically be closed after 1 second if not registered.
        // Could be handled better, but this will do for now.
      }
    case Terminated(child) =>
      log.info(s"Child $child has been terminated.")
      log.info(s"Corresponding slot: ${slotAssignments.get(child)}")
      unassignSlot(child)
      for (simulator <- runningGame)
        stopGame(simulator)
    case GameTimedOut(simulator) =>
      for (
        currentSimulator <- runningGame
        if currentSimulator == simulator
      ) stopGame(simulator)
    case GetStatus =>
      sender() ! Status(clients.size, runningGame.size, freeSlots.size)
    case Stop =>
      for (simulator <- runningGame)
        stopGame(simulator)
      context.stop(self)
  }

  private def acceptConnection(rawConnection: ActorRef): Connection = {
    val slot = popSlot()
    val worker = new RemoteWebsocketClient(slot.players, map)
    val websocketActor = context.actorOf(WebsocketActor.props(rawConnection, worker))
    rawConnection ! Http.Register(websocketActor)
    clients += worker
    context.watch(websocketActor)

    val connection = Connection(rawConnection, websocketActor, worker, slot)
    assignSlot(connection)
    connection
  }

  private def startGame(): Unit = {
    val simulator = new DroneWorldSimulator(
      map,
      Seq(new DummyDroneController, new DummyDroneController),
      t => Seq.empty,
      None,
      AuthoritativeServerConfig(Set.empty, Set(OrangePlayer, BluePlayer), clients)
    )
    simulator.onException((e: Throwable) => {
      log.info(s"Terminating running multiplayer game because of uncaught exception.")
      log.info(s"Exception message ${e.getMessage}")
      stopGame(simulator)
    })
    context.system.scheduler.scheduleOnce(5 minutes, self, GameTimedOut(simulator))
    runningGame = Some(simulator)
    simulator.run()
  }

  private def assignSlot(connection: Connection): Unit = {
    slotAssignments += connection.websocketActor -> connection
  }

  private def unassignSlot(connection: ActorRef): Unit = {
    val Connection(_, _, worker, slot) = slotAssignments(connection)
    freeSlots :+= slot
    clients -= worker
    slotAssignments -= connection
  }

  private def stopGame(simulator: DroneWorldSimulator): Unit = {
    simulator.terminate()
    if (runningGame.contains(simulator)) runningGame = None
    for ((websocketActor, connection) <- slotAssignments) {
      context.unwatch(websocketActor)
      context.stop(connection.rawConnection)
      unassignSlot(websocketActor)
    }
  }

  case class MultiplayerSlot(player: Player) {
    val players: Set[Player] = Set(player)
  }

  private def popSlot(): MultiplayerSlot = {
    val result = freeSlots.head
    freeSlots = freeSlots.tail
    result
  }

  def freeSlotCount: Int = freeSlots.length

  def hasFreeSlot: Boolean = freeSlots.nonEmpty
}


private[codecraft] object MultiplayerServer {
  def props(displayGame: Boolean = false) = Props(classOf[MultiplayerServer], displayGame)
}

