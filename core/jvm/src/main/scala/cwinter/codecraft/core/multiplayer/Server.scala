package cwinter.codecraft.core.multiplayer

import java.nio.ByteBuffer

import akka.actor._
import akka.io.IO
import cwinter.codecraft.core.api._
import cwinter.codecraft.core.game._
import cwinter.codecraft.core.objects.drone.{GameClosed, ServerBusy, ServerMessage}
import cwinter.codecraft.core.replay.DummyDroneController
import cwinter.codecraft.graphics.engine.JVMAsyncRunner
import org.joda.time.DateTime
import spray.can.Http
import spray.can.server.UHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.ref.WeakReference

object Server {
  implicit val system = ActorSystem()

  def spawnServerInstance2(
                            seed: Int = scala.util.Random.nextInt,
                            mapGenerator: => WorldMap = TheGameMaster.defaultMap,
                            displayGame: Boolean = false,
                            recordReplaysToFile: Boolean = false,
                            maxGames: Int = 10,
                            winConditions: Seq[WinCondition] = WinCondition.default
                          ): Unit = {
    start(seed, mapGenerator, displayGame, recordReplaysToFile, maxGames)
    system.awaitTermination()
  }

  def start(
             seed: Int = scala.util.Random.nextInt,
             mapGenerator: => WorldMap = TheGameMaster.defaultMap,
             displayGame: Boolean = false,
             recordReplaysToFile: Boolean = false,
             maxGames: Int = 10,
             winConditions: Seq[WinCondition] = WinCondition.default
           ): ActorRef = {
    val server = system.actorOf(Props(classOf[MultiplayerServer],
      seed,
      () => mapGenerator,
      displayGame,
      recordReplaysToFile,
      maxGames,
      winConditions))
    IO(UHttp) ! Http.Bind(server, "0.0.0.0", 8080)
    server
  }

  def main(args: Array[String]): Unit = {
    spawnServerInstance2(displayGame = false)
  }

  object Stop

  object GetStatus

  object GetDetailedStatus

  object ScrewThis

  case class MatchmakingRequest(client: WebsocketClientConnection)

}

class MultiplayerServer(
                         private var nextRNGSeed: Int = scala.util.Random.nextInt,
                         val mapGenerator: () => WorldMap = () => TheGameMaster.defaultMap,
                         val displayGame: Boolean = false,
                         val recordReplaysToFile: Boolean = false,
                         val maxGames: Int = 10,
                         val winConditions: Seq[WinCondition] = WinCondition.default
                       ) extends Actor
  with ActorLogging {

  import Server._

  val tickPeriod = 10
  val startTimestamp = new DateTime().getMillis

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
                       connections: Seq[Connection],
                       startTimestamp: Long
                     )

  case class GameTimedOut(simulatorRef: WeakReference[DroneWorldSimulator])

  def receive = {
    case ScrewThis => sender() ! this
    case m@Http.Connected(remoteAddress, localAddress) =>
      val rawConnection = sender()
      if (connectionInfo.size < 2 * maxGames) {
        val connection = acceptConnection(rawConnection)
        connectionInfo += connection.websocketActor -> connection
      } else {
        rejectConnection(rawConnection)
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
    case GameTimedOut(simulatorRef) =>
      simulatorRef.get match {
        case Some(simulator) => if (runningGames.contains(simulator)) stopGame(simulator, GameClosed.Timeout)
        case None =>
      }
    case GetStatus =>
      sender() ! Status(waitingClient.nonEmpty, runningGames.size, connectionInfo.size, maxGames * 2)
    case GetDetailedStatus =>
      val gameDetails =
        for ((sim, info) <- runningGames)
          yield gameStatus(sim, info)
      sender() ! DetailedStatus(waitingClient.nonEmpty,
        connectionInfo.size,
        gameDetails.toSeq ++ completedGames,
        new DateTime().getMillis,
        startTimestamp)
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

  def startLocalGame(c1: DroneControllerBase, c2: DroneControllerBase): DroneWorldSimulator = {
    log.info("Starting Local Game")
    val map = mapGenerator()
    var remotePlayers = Set.empty[Player]
    var remoteClients = Set.empty[RemoteClient]
    var controllers = Seq(c1, c2)
    var connections = Seq.empty[Connection]

    for (observer <- waitingClient) {
      log.info("Observer Joined")
      val player = Observer(3)
      remotePlayers = Set(player)
      controllers = Seq(c1, c2, new DummyDroneController())
      remoteClients = Set(observer.worker.asInstanceOf[RemoteClient])
      connections = Seq(observer)
      observer.worker.initialise(Set(player), map, nextRNGSeed, tickPeriod, WinCondition.default)
      waitingClient = None
    }

    val simulator = new DroneWorldSimulator(
      map.createGameConfig(
        controllers,
        tickPeriod = tickPeriod,
        rngSeed = nextRNGSeed,
        winConditions = winConditions
      ),
      multiplayerConfig = AuthoritativeServerConfig(
        Set(BluePlayer, OrangePlayer),
        remotePlayers,
        remoteClients,
        updateCompleted,
        onTimeout),
      settings = Settings.default.copy(recordReplays = false)
    ) with JVMAsyncRunner
    simulator.graphicsEnabled = displayGame
    nextRNGSeed = scala.util.Random.nextInt
    simulator.framerateTarget = if (displayGame) 60 else 1001
    simulator.onException((e: Throwable) => {
      log.info(s"Terminating running multiplayer game because of uncaught exception.")
      log.info(s"Exception message:\n${e.getStackTrace.mkString("\n")}")
      stopGame(simulator, GameClosed.Crash(e.getMessage + "\n" + e.getStackTrace.mkString("\n")))
    })
    context.system.scheduler.scheduleOnce(20 minutes, self, GameTimedOut(WeakReference(simulator)))
    runningGames += simulator -> GameInfo(connections, new DateTime().getMillis)

    for (c <- connections) c.assignedGame = Some(simulator)

    if (displayGame) TheGameMaster.run(simulator) else simulator.runAsync()

    simulator
  }

  private def acceptConnection(rawConnection: ActorRef): Connection = {
    val worker = new WebsocketClientConnection(self)
    val websocketActor = context.actorOf(WebsocketActor.props(rawConnection, worker))
    rawConnection ! Http.Register(websocketActor)
    context.watch(websocketActor)
    val connection = Connection(rawConnection, websocketActor, worker)
    context.system.scheduler.scheduleOnce(10 minutes, new Runnable {
      override def run(): Unit = {
        if (!hasStartedMatchmaking(rawConnection)) {
          if (context != null) {
            context.stop(rawConnection)
            context.stop(websocketActor)
          }
          connectionInfo -= connection.websocketActor
        }
      }
    })
    connection
  }

  private def hasStartedMatchmaking(rawConnection: ActorRef): Boolean =
    waitingClient.exists(_.rawConnection == rawConnection) ||
      runningGames.valuesIterator.exists(_.connections.exists(_.rawConnection == rawConnection))

  private def rejectConnection(rawConnection: ActorRef): Unit = {
    class RejectConnection extends WebsocketWorker {
      var closed = false

      override def receiveBytes(bytes: ByteBuffer): Unit = close()

      override def receive(message: String): Unit = close()

      def close(): Unit = if (!closed) {
        send(ServerMessage.serializeBinary(ServerBusy))
        closeConnection()
        closed = true
      }
    }
    val worker = new RejectConnection
    val websocketActor = context.actorOf(WebsocketActor.props(rawConnection, worker))
    rawConnection ! Http.Register(websocketActor)
    context.system.scheduler.scheduleOnce(5 seconds, new Runnable {
      override def run(): Unit = worker.close()
    })
    context.system.scheduler.scheduleOnce(10 seconds, new Runnable {
      override def run(): Unit = {
        context.stop(rawConnection)
        context.stop(websocketActor)
      }
    })
  }

  private def startGame(connections: Connection*): Unit = {
    val map = mapGenerator()
    connections.zip(Seq(BluePlayer, OrangePlayer)).foreach {
      case (connection, player) =>
        connection.worker.initialise(Set(player), map, nextRNGSeed, tickPeriod, WinCondition.default)
    }
    val clients = connections.map(_.worker.asInstanceOf[RemoteClient]).toSet
    val simulator = new DroneWorldSimulator(
      map.createGameConfig(
        Seq(new DummyDroneController, new DummyDroneController),
        tickPeriod = tickPeriod,
        rngSeed = nextRNGSeed,
        winConditions = winConditions
      ),
      multiplayerConfig = AuthoritativeServerConfig(Set.empty,
        clients.flatMap(_.players),
        clients,
        updateCompleted,
        onTimeout),
      settings = Settings.default.copy(recordReplays = false)
    ) with JVMAsyncRunner
    simulator.graphicsEnabled = displayGame
    nextRNGSeed = scala.util.Random.nextInt
    simulator.framerateTarget = if (displayGame) 60 else 1001
    simulator.onException((e: Throwable) => {
      log.info(s"Terminating running multiplayer game because of uncaught exception.")
      log.info(s"Exception message:\n${e.getStackTrace.mkString("\n")}")
      stopGame(simulator, GameClosed.Crash(e.getMessage + "\n" + e.getStackTrace.mkString("\n")))
    })
    context.system.scheduler.scheduleOnce(20 minutes, self, GameTimedOut(WeakReference(simulator)))
    runningGames += simulator -> GameInfo(connections, new DateTime().getMillis)
    for (c <- connections) c.assignedGame = Some(simulator)
    if (displayGame) TheGameMaster.run(simulator) else simulator.runAsync()
  }

  private def updateCompleted(simulator: DroneWorldSimulator): Unit =
    if (runningGames.contains(simulator))
      for (winner <- simulator.winner)
        stopGame(simulator, GameClosed.PlayerWon(winner.id))

  private def onTimeout(simulator: DroneWorldSimulator): Unit =
    if (runningGames.contains(simulator))
      stopGame(simulator, GameClosed.PlayerTimedOut)

  private def stopGame(simulator: DroneWorldSimulator, reason: GameClosed.Reason): Unit = synchronized {
    runningGames.get(simulator) match {
      case Some(info) =>
        simulator.terminate()
        completedGames ::= gameStatus(simulator, info, Some(reason.toString))
        runningGames -= simulator
        for (Connection(rawConnection, websocketActor, worker) <- info.connections) {
          connectionInfo -= websocketActor
          context.unwatch(websocketActor)
          worker.close(reason)
          context.system.scheduler.scheduleOnce(15 seconds, new Runnable {
            override def run(): Unit = {
              context.stop(rawConnection)
              context.stop(websocketActor)
            }
          })
        }
      case None =>
    }
  }

  private def gameStatus(sim: DroneWorldSimulator, info: GameInfo, closeReason: Option[String] = None) = {
    val nowMS = new DateTime().getMillis
    val outBandwidth =
      info.connections.foldLeft(0.0) {
        case (sum, c) =>
          if (closeReason.isEmpty) sum + c.worker.outKbps(sim.measuredFramerate / tickPeriod.toDouble)
          else sum + c.worker.totalBytesOut
      }
    val inBandwidth =
      info.connections.foldLeft(0.0) {
        case (sum, c) =>
          if (closeReason.isEmpty) sum + c.worker.inKbps(sim.measuredFramerate / tickPeriod.toDouble)
          else sum + c.worker.totalBytesIn
      }
    GameStatus(closeReason,
      sim.measuredFramerate,
      (1000 * sim.timestep / math.max(1, nowMS - info.startTimestamp)).toInt,
      sim.timestep,
      info.startTimestamp,
      closeReason.map(_ => nowMS),
      if (info.connections.isEmpty) 0 else info.connections.map(_.worker.msSinceLastResponse).max,
      sim.currentPhase.toString,
      outBandwidth,
      inBandwidth)
  }
}
