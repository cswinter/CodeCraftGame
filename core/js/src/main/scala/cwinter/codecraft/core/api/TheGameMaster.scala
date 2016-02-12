package cwinter.codecraft.core.api

import cwinter.codecraft.core.multiplayer.{WebsocketClient, JSWebsocketClient, WebsocketServerConnection}
import cwinter.codecraft.core.{DroneWorldSimulator, MultiplayerClientConfig, MultiplayerConfig, WorldMap}
import cwinter.codecraft.graphics.engine.{Debug, WebGLRenderer}
import cwinter.codecraft.graphics.model.TheModelCache
import cwinter.codecraft.util.maths.ColorRGBA
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
  private var running: Future[Unit] = Future.successful(Unit)
  private[this] var runContext: Option[RunContext] = None
  private[codecraft] var outputFPS: Boolean = false


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
    context.fps.computeCurrFPS()
    if (outputFPS && !context.simulator.isPaused) {
      context.fps.drawFPS()
      if (context.simulator.timestep % 100 == 0) context.fps.printFPS()
    }
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

  override def connectToWebsocket(connectionString: String): WebsocketClient =
    new JSWebsocketClient(connectionString)
}

class RunContext(
  val simulator: DroneWorldSimulator,
  val renderer: WebGLRenderer,
  val targetMillisPerFrame: Int,
  var lastCompletionTime: Double = js.Date.now()
) {
  val fps = new FPSMeter(this)
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

class FPSMeter(context: RunContext) {
  val startTime = js.Date.now()
  var lastTime = js.Date.now()
  var fps = 0


  def drawFPS(): Unit = {
    Debug.drawText(fpsString, -1, 1, ColorRGBA(1, 1, 1, 1), true, false)
  }

  def printFPS(): Unit = println(fpsString)

  def fpsString: String = {
    val elapsedSeconds = (js.Date.now() - startTime) / 1000
    s"T=${context.simulator.timestep}," +
      s" Average FPS: ${(context.simulator.timestep / elapsedSeconds).toInt}, " +
      s"FPS: $fps"
  }

  def computeCurrFPS(): Unit = {
    if (context.simulator.timestep % 30 == 0) {
      fps = math.round(30 * 1000 / (js.Date.now() - lastTime)).toInt
      lastTime = js.Date.now()
    }
  }
}

