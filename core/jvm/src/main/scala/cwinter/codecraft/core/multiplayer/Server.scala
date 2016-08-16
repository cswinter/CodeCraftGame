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
    displayGame: Boolean = false,
    maxGames: Int = 10
  ): Unit = {
    implicit val system = ActorSystem()
    val server = system.actorOf(
      Props(classOf[MultiplayerServer], seed, map, displayGame, maxGames), "websocket")
    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)
    system.awaitTermination()
  }

  def start(): ActorRef = {
    implicit val system = ActorSystem()
    val server = system.actorOf(Props(classOf[MultiplayerServer]), "websocket")
    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)
    server
  }

  def main(args: Array[String]): Unit = {
    spawnServerInstance(displayGame = false)
  }


  object Stop
  object GetStatus
  case class Status(
    clientWaiting: Boolean,
    runningGames: Int
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
        AuthoritativeServerConfig(serverPlayers, clientPlayers, Set(worker), s => (), s => ()),
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

private[codecraft] class MultiplayerServer(
  private var nextRNGSeed: Int = scala.util.Random.nextInt,
  val map: WorldMap = TheGameMaster.defaultMap,
  val displayGame: Boolean = false,
  val maxGames: Int = 10
) extends Actor with ActorLogging {
  import Server.{GetStatus, Status, Stop}

  private var waitingClient = Option.empty[RemoteClient]
  private var connectionInfo = Map.empty[ActorRef, Connection]
  private var runningGames = Set.empty[DroneWorldSimulator]

  case class Connection(
    rawConnection: ActorRef,
    websocketActor: ActorRef,
    worker: WebsocketClientConnection
  ) {
    var assignedGame: Option[DroneWorldSimulator] = None
  }

  case class GameTimedOut(simulator: DroneWorldSimulator)


  def receive = {
    case m@Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      if (runningGames.size < maxGames) {
        val connection = acceptConnection(serverConnection)
        connectionInfo += connection.websocketActor -> connection
        waitingClient match {
          case None =>
            waitingClient = Some(connection.worker)
          case Some(c) =>
            startGame(connection.worker, c)
            waitingClient = None
        }
      } else {
        // FIXME: Connection is automatically closed after 1 second since we don't register it.
        // Instead, we should give something like a "ServerFull" reply and then close the connection immediately.
      }
    case Terminated(websocketActor) =>
      val connectionOpt = connectionInfo.get(websocketActor)
      connectionInfo -= websocketActor
      for {
        connection <- connectionOpt
        game <- connection.assignedGame
        disconnectedPlayer = connection.worker.players.head.id
      } stopGame(game, GameClosed.PlayerDisconnected(disconnectedPlayer))

      log.info(s"Child $websocketActor has been terminated.")
      log.info(s"Corresponding connection: $connectionOpt")
    case GameTimedOut(simulator) =>
      if (runningGames.contains(simulator))
        stopGame(simulator, GameClosed.Timeout)
    case GetStatus =>
      sender() ! Status(waitingClient.isEmpty, runningGames.size)
    case Stop =>
      for (simulator <- runningGames)
        stopGame(simulator, GameClosed.ServerStopped)
      context.stop(self)
  }

  private def acceptConnection(rawConnection: ActorRef): Connection = {
    val worker = new WebsocketClientConnection(nextPlayer, map, nextRNGSeed)
    val websocketActor = context.actorOf(WebsocketActor.props(rawConnection, worker))
    rawConnection ! Http.Register(websocketActor)
    context.watch(websocketActor)
    Connection(rawConnection, websocketActor, worker)
  }

  private def nextPlayer: Set[Player] = if (waitingClient.isEmpty) Set(BluePlayer) else Set(OrangePlayer)

  private def startGame(clients: RemoteClient*): Unit = {
    val simulator = new DroneWorldSimulator(
      map,
      Seq(new DummyDroneController, new DummyDroneController),
      t => Seq.empty,
      None,
      AuthoritativeServerConfig(
        Set.empty, clients.flatMap(_.players).toSet, clients.toSet, updateCompleted, onTimeout),
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
    runningGames += simulator
    if (displayGame) TheGameMaster.run(simulator) else simulator.run()
  }

  private def updateCompleted(simulator: DroneWorldSimulator): Unit =
    if (runningGames.contains(simulator))
      for (winner <- simulator.winner)
        stopGame(simulator, GameClosed.PlayerWon(winner.id))

  private def onTimeout(simulator: DroneWorldSimulator): Unit =
    if (runningGames.contains(simulator))
      stopGame(simulator, GameClosed.PlayerTimedOut)

  private def stopGame(simulator: DroneWorldSimulator, reason: GameClosed.Reason): Unit = {
    simulator.terminate()
    if (runningGames.contains(simulator)) runningGames -= simulator
    for ((websocketActor, connection) <- connectionInfo) {
      context.unwatch(websocketActor)
      connection.worker.close(reason)
      context.system.scheduler.scheduleOnce(15 seconds, new Runnable {
        override def run(): Unit =  context.stop(connection.rawConnection)
      })
    }
  }
}


private[codecraft] object SinglePlayerMultiplayerServer {
  def props(seed: Option[Int], displayGame: Boolean = false) = Props(classOf[SinglePlayerMultiplayerServer], seed, displayGame)
}

