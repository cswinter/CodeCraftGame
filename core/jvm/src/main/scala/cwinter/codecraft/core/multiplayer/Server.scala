package cwinter.codecraft.core.multiplayer

import akka.actor._
import akka.io.IO
import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.game.{AuthoritativeServerConfig, DroneWorldSimulator, WorldMap}
import cwinter.codecraft.core.objects.drone.GameClosed
import cwinter.codecraft.core.replay.DummyDroneController
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object Server {
  def spawnServerInstance(seed: Option[Int] = None, displayGame: Boolean = false): Unit = {
    implicit val system = ActorSystem()
    val server = system.actorOf(SinglePlayerMultiplayerServer.props(seed, displayGame), "websocket")
    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)
    system.awaitTermination()
  }

  def spawnServerInstance2(
    seed: Int = scala.util.Random.nextInt,
    map: WorldMap = TheGameMaster.defaultMap,
    displayGame: Boolean = false
  ): Unit = {
    implicit val system = ActorSystem()
    val server = system.actorOf(Props(classOf[TwoPlayerMultiplayerServer], seed, map, displayGame), "websocket")
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
    spawnServerInstance(displayGame = false)
  }


  object Stop
  object GetStatus
  case class Status(
    connections: Int,
    runningGames: Int,
    freeSlots: Int
  )
}


private[codecraft] class SinglePlayerMultiplayerServer(seed: Option[Int], displayGame: Boolean = false) extends Actor with ActorLogging {
  val map = TheGameMaster.defaultMap
  val clientPlayers = Set[Player](BluePlayer)
  val serverPlayers = Set[Player](OrangePlayer)


  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val rngSeed = this.seed.getOrElse(scala.util.Random.nextInt)
      val worker = new WebsocketClientConnection(clientPlayers, map, rngSeed)
      val conn = context.actorOf(WebsocketActor.props(serverConnection, worker))

      val server = new DroneWorldSimulator(
        map,
        Seq(new DummyDroneController, TheGameMaster.destroyerAI()),
        t => Seq.empty,
        None,
        AuthoritativeServerConfig(serverPlayers, clientPlayers, Set(worker), s => ()),
        rngSeed = rngSeed
      )
      server.framerateTarget = 1001

      serverConnection ! Http.Register(conn)

      if (displayGame) {
        TheGameMaster.run(server)
      } else {
        server.run()
      }
  }
}

private[codecraft] class TwoPlayerMultiplayerServer(
  private var nextRNGSeed: Int = scala.util.Random.nextInt,
  val map: WorldMap = TheGameMaster.defaultMap,
  val displayGame: Boolean = false
) extends Actor with ActorLogging {
  import Server.{GetStatus, Status, Stop}

  private var clients = Set.empty[RemoteClient]
  private var freeSlots = Seq(MultiplayerSlot(OrangePlayer), MultiplayerSlot(BluePlayer))
  private var slotAssignments = Map.empty[ActorRef, Connection]
  private var runningGame: Option[DroneWorldSimulator] = None

  case class Connection(
    rawConnection: ActorRef,
    websocketActor: ActorRef,
    worker: WebsocketClientConnection,
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
      for (simulator <- runningGame) {
        val disconnectedPlayer = slotAssignments.get(child).map(_.worker.players.head.id).getOrElse(-1)
        stopGame(simulator, GameClosed.PlayerDisconnected(disconnectedPlayer))
      }
    case GameTimedOut(simulator) =>
      if (runningGame.contains(simulator)) {
        stopGame(simulator, GameClosed.Timeout)
      }
    case GetStatus =>
      sender() ! Status(clients.size, runningGame.size, freeSlots.size)
    case Stop =>
      for (simulator <- runningGame)
        stopGame(simulator, GameClosed.ServerStopped)
      context.stop(self)
  }

  private def acceptConnection(rawConnection: ActorRef): Connection = {
    val slot = popSlot()
    val worker = new WebsocketClientConnection(slot.players, map, nextRNGSeed)
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
      AuthoritativeServerConfig(Set.empty, Set(OrangePlayer, BluePlayer), clients, updateCompleted),
      rngSeed = nextRNGSeed
    )
    simulator.graphicsEnabled = displayGame
    nextRNGSeed = scala.util.Random.nextInt
    simulator.framerateTarget = if (displayGame) 60 else 1001
    simulator.onException((e: Throwable) => {
      log.info(s"Terminating running multiplayer game because of uncaught exception.")
      log.info(s"Exception message:\n${e.getStackTrace.mkString("\n")}")
      stopGame(simulator, GameClosed.Crash(e.getMessage + "\n" + e.getStackTrace.mkString("\n")))
    })
    context.system.scheduler.scheduleOnce(20 minutes, self, GameTimedOut(simulator))
    runningGame = Some(simulator)
    if (displayGame) TheGameMaster.run(simulator) else simulator.run()
  }

  private def updateCompleted(simulator: DroneWorldSimulator): Unit = {
    if (runningGame.contains(simulator)) {
      for (winner <- simulator.winner) {
        stopGame(simulator, GameClosed.PlayerWon(winner.id))
      }
    }
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

  private def stopGame(simulator: DroneWorldSimulator, reason: GameClosed.Reason): Unit = {
    simulator.terminate()
    if (runningGame.contains(simulator)) runningGame = None
    for ((websocketActor, connection) <- slotAssignments) {
      context.unwatch(websocketActor)
      connection.worker.close(reason)
      context.system.scheduler.scheduleOnce(15 seconds, new Runnable {
        override def run(): Unit =  context.stop(connection.rawConnection)
      })
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


private[codecraft] object SinglePlayerMultiplayerServer {
  def props(seed: Option[Int], displayGame: Boolean = false) = Props(classOf[SinglePlayerMultiplayerServer], seed, displayGame)
}

