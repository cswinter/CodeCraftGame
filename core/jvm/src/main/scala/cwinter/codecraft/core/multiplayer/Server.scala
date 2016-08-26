package cwinter.codecraft.core.multiplayer

import akka.actor._
import akka.io.IO
import cwinter.codecraft.core.api.{BluePlayer, OrangePlayer, Player, TheGameMaster}
import cwinter.codecraft.core.game.{WinCondition, AuthoritativeServerConfig, DroneWorldSimulator, WorldMap}
import cwinter.codecraft.core.objects.drone.GameClosed
import cwinter.codecraft.core.replay.DummyDroneController
import org.joda.time.DateTime
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object Server {
  def spawnServerInstance2(
    seed: Int = scala.util.Random.nextInt,
    map: WorldMap = TheGameMaster.defaultMap,
    displayGame: Boolean = false,
    maxGames: Int = 10
  ): Unit = {
    start(seed, map, displayGame, maxGames)
    ActorSystem().awaitTermination()
  }

  def start(
    seed: Int = scala.util.Random.nextInt,
    map: WorldMap = TheGameMaster.defaultMap,
    displayGame: Boolean = false,
    maxGames: Int = 10
  ): ActorRef = {
    implicit val system = ActorSystem()
    val server =
      system.actorOf(Props(classOf[MultiplayerServer], seed, map, displayGame, maxGames), "websocket")
    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)
    server
  }

  def main(args: Array[String]): Unit = {
    spawnServerInstance2(displayGame = false)
  }

  object Stop
  object GetStatus
  object GetDetailedStatus
  case class MatchmakingRequest(client: WebsocketClientConnection)
}

private[codecraft] class MultiplayerServer(
  private var nextRNGSeed: Int = scala.util.Random.nextInt,
  val map: WorldMap = TheGameMaster.defaultMap,
  val displayGame: Boolean = false,
  val maxGames: Int = 10
) extends Actor
    with ActorLogging {
  import Server._
  val tickPeriod = 10

  private var waitingClient = Option.empty[Connection]
  private var connectionInfo = Map.empty[ActorRef, Connection]
  private var runningGames = Map.empty[DroneWorldSimulator, GameInfo]
  private var completedGames = List.empty[GameStatus]

  case class Connection(
    rawConnection: ActorRef,
    websocketActor: ActorRef,
    worker: WebsocketClientConnection
  ) {
    var assignedGame: Option[DroneWorldSimulator] = None
  }

  case class GameInfo(
    val connections: Seq[Connection],
    val startTimestamp: Long
  )

  case class GameTimedOut(simulator: DroneWorldSimulator)

  def receive = {
    case m @ Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      if (runningGames.size < maxGames) {
        val connection = acceptConnection(serverConnection)
        connectionInfo += connection.websocketActor -> connection
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
      if (connectionOpt == waitingClient) waitingClient = None

      log.info(s"Child $websocketActor has been terminated.")
      log.info(s"Corresponding connection: $connectionOpt")
    case GameTimedOut(simulator) =>
      if (runningGames.contains(simulator))
        stopGame(simulator, GameClosed.Timeout)
    case GetStatus =>
      sender() ! Status(waitingClient.nonEmpty, runningGames.size)
    case GetDetailedStatus =>
      val gameDetails =
        for ((sim, info) <- runningGames)
          yield GameStatus(None, sim.measuredFramerate, sim.timestep, info.startTimestamp)
      sender() ! DetailedStatus(waitingClient.nonEmpty,
                                connectionInfo.size,
                                gameDetails.toSeq ++ completedGames,
                                new DateTime().getMillis)
    case Stop =>
      for (simulator <- runningGames.keys)
        stopGame(simulator, GameClosed.ServerStopped)
      context.stop(self)
    case MatchmakingRequest(client) =>
      val connection = connectionInfo.valuesIterator.find(_.worker == client).get
      waitingClient match {
        case None => waitingClient = Some(connection)
        case Some(c) =>
          startGame(connection, c)
          waitingClient = None
      }
  }

  private def acceptConnection(rawConnection: ActorRef): Connection = {
    val worker = new WebsocketClientConnection(self)
    val websocketActor = context.actorOf(WebsocketActor.props(rawConnection, worker))
    rawConnection ! Http.Register(websocketActor)
    context.watch(websocketActor)
    Connection(rawConnection, websocketActor, worker)
  }

  private def startGame(connections: Connection*): Unit = {
    connections.zip(Seq(BluePlayer, OrangePlayer)).foreach {
      case (connection, player) =>
        connection.worker.initialise(Set(player), map, nextRNGSeed, tickPeriod, WinCondition.default)
    }
    val clients = connections.map(_.worker.asInstanceOf[RemoteClient]).toSet
    val simulator = new DroneWorldSimulator(
      map.createGameConfig(
        Seq(new DummyDroneController, new DummyDroneController),
        tickPeriod = tickPeriod,
        rngSeed = nextRNGSeed
      ),
      multiplayerConfig =
        AuthoritativeServerConfig(Set.empty, clients.flatMap(_.players), clients, updateCompleted, onTimeout)
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
    runningGames += simulator -> GameInfo(connections, new DateTime().getMillis)
    for (c <- connections) c.assignedGame = Some(simulator)
    if (displayGame) TheGameMaster.run(simulator) else simulator.run()
  }

  private def updateCompleted(simulator: DroneWorldSimulator): Unit =
    if (runningGames.contains(simulator))
      for (winner <- simulator.winner)
        stopGame(simulator, GameClosed.PlayerWon(winner.id))

  private def onTimeout(simulator: DroneWorldSimulator): Unit =
    if (runningGames.contains(simulator))
      stopGame(simulator, GameClosed.PlayerTimedOut)

  private def stopGame(simulator: DroneWorldSimulator, reason: GameClosed.Reason) = synchronized {
    runningGames.get(simulator) match {
      case Some(info) =>
        simulator.terminate()
        completedGames ::=
          GameStatus(Some(reason.toString),
            simulator.measuredFramerate,
            simulator.timestep,
            info.startTimestamp)
        runningGames -= simulator
        for (Connection(rawConnection, websocketActor, worker) <- info.connections) {
          connectionInfo -= websocketActor
          context.unwatch(websocketActor)
          worker.close(reason)
          context.system.scheduler.scheduleOnce(15 seconds, new Runnable {
            override def run(): Unit = context.stop(rawConnection)
          })
        }
      case None =>
    }
  }
}
