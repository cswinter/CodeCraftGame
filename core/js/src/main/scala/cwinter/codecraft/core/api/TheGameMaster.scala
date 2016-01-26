package cwinter.codecraft.core.api

import cwinter.codecraft.core.multiplayer.{JSWebsocketClient, WebsocketServerConnection}
import cwinter.codecraft.core.{DroneWorldSimulator, MultiplayerClientConfig, MultiplayerConfig, WorldMap}
import cwinter.codecraft.graphics.engine.{Debug, WebGLRenderer}
import cwinter.codecraft.graphics.model.TheModelCache
import org.scalajs.dom
import org.scalajs.dom.html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}
import scala.util.{Failure, Success}

/**
 * Main entry point to start the game.
 */
@JSExport
@JSExportAll
object TheGameMaster extends GameMasterLike {
  var canvas: html.Canvas = null
  private[this] var intervalID: Option[Int] = None
  private[this] var runContext: Option[RunContext] = None


  def run(simulator: DroneWorldSimulator): DroneWorldSimulator = {
    require(canvas != null, "Must first set TheGameMaster.canvas variable to the webgl canvas element.")
    require(intervalID.isEmpty && runContext.isEmpty, "Can only run one CodeCraft game at a time.")

    val renderer = new WebGLRenderer(canvas, simulator, simulator.map.initialDrones.head.position)
    val context = new RunContext(simulator, renderer, 16)
    runContext = Some(context)
    run(context)
    simulator
  }

  def run(context: RunContext): Unit = {
    context.renderer.render()
    val updateFuture =
      if (context.simulator.isPaused) Future.successful(Unit)
      else context.simulator.performAsyncUpdate()

    updateFuture.onComplete {
      case Success(_) =>
        if (!context.stopped) {
          dom.setTimeout(
            () => run(context),
            context.computeWaitTime()
          )
        }
      case Failure(x) =>
        println("Uncaught exception thrown by game engine:")
        println(x.getMessage)
    }
  }

  def stop(): Unit = {
    dom.clearInterval(intervalID.get)
    intervalID = None
    runContext.foreach(_.stop())
    TheModelCache.clear()
    Debug.clearDrawAlways()
  }

  def prepareMultiplayerGame(serverAddress: String): Future[(WorldMap, MultiplayerConfig)] = {
    val websocketConnection = new JSWebsocketClient(s"ws://$serverAddress:8080")
    val serverConnection = new WebsocketServerConnection(websocketConnection)
    val sync = serverConnection.receiveInitialWorldState()

    // TODO: receive this information from server
    val clientPlayers = Set[Player](BluePlayer)
    val serverPlayers = Set[Player](OrangePlayer)

    sync.map(sync => {
      val map = sync.worldMap
      val connection = MultiplayerClientConfig(clientPlayers, serverPlayers, serverConnection)
      (map, connection)
    })
  }
}

class RunContext(
  val simulator: DroneWorldSimulator,
  val renderer: WebGLRenderer,
  val targetMillisPerFrame: Int,
  var lastCompletionTime: Double = js.Date.now()
) {
  private[this] var _stopped = false
  def stopped: Boolean = _stopped
  def stop(): Unit = _stopped = true

  def computeWaitTime(): Double = {
    val time = js.Date.now()
    val elapsed = time - lastCompletionTime
    lastCompletionTime = elapsed
    math.max(0, targetMillisPerFrame - elapsed)
  }
}


