package cwinter.codecraft.core.api

import cwinter.codecraft.core.game.DroneWorldSimulator
import cwinter.codecraft.core.multiplayer.{JSWebsocketClient, WebsocketClient}
import cwinter.codecraft.graphics.engine.WebGLRenderer
import cwinter.codecraft.util.maths.ColorRGBA
import org.scalajs.dom
import org.scalajs.dom.html

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}

/**
 * Main entry point to start the game.
 */
@JSExport
@JSExportAll
object TheGameMaster extends GameMasterLike {
  var canvas: html.Canvas = null
  private[this] var runContext: Option[RunContext] = None
  private[codecraft] var outputFPS: Boolean = false


  def run(simulator: DroneWorldSimulator): DroneWorldSimulator = {
    require(canvas != null, "Must first set TheGameMaster.canvas variable to the webgl canvas element.")
    require(runContext.isEmpty, "Can only run one CodeCraft game at a time.")

    val renderer = new WebGLRenderer(canvas, simulator, simulator.map.initialDrones.head.position)
    val context = new RunContext(simulator, renderer, 16)
    runContext = Some(context)
    run(context)
    simulator
  }


  private def run(context: RunContext): Unit = {
    import context._
    if (stopped) return
    dom.requestAnimationFrame((d: Double) => run(context))
    if (simulator.isCurrentlyUpdating) { println(s"Skipped frame at ${simulator.timestep}"); return }

    if (!fps.shouldSkipFrame(simulator.framerateTarget)) {
      fps.startedFrame(simulator.framerateTarget)

      fps.updateSmoothedFPS()
      if (outputFPS && !simulator.isPaused) {
        fps.drawFPS()
        if (simulator.timestep % 100 == 0) fps.printFPS()
      }

      renderer.render()
      if (!simulator.isPaused) simulator.performAsyncUpdate()
    }
  }

  def stop(): Unit = {
    runContext.foreach(_.stop())
    runContext = None
  }

  override def connectToWebsocket(connectionString: String): WebsocketClient =
    new JSWebsocketClient(connectionString)


  def currentFPS: Option[Int] = runContext.map(_.fps.fps)
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
  def stop(): Unit = {
    _stopped = true
    renderer.dispose()
  }

  def computeWaitTime(): Double = {
    val time = js.Date.now()
    val elapsed = time - lastCompletionTime
    lastCompletionTime = time
    println(s"computeWaitTime: $time, $lastCompletionTime, $elapsed ${math.max(0, targetMillisPerFrame - elapsed)}")
    math.max(0, targetMillisPerFrame - elapsed)
  }
}

class FPSMeter(context: RunContext) {
  val startTime = js.Date.now()
  var last60FrameStarted = js.Date.now()
  var lastFrameStarted = js.Date.now()
  var fps = 0


  def drawFPS(): Unit = {
    context.simulator.debug.drawText(fpsString, -1, 1, ColorRGBA(1, 1, 1, 1), true, false, false)
  }

  def printFPS(): Unit = println(fpsString)

  def fpsString: String = {
    val elapsedSeconds = (js.Date.now() - startTime) / 1000
    s"T=${context.simulator.timestep}," +
      s" Average FPS: ${(context.simulator.timestep / elapsedSeconds).toInt}, " +
      s"FPS: $fps"
  }

  def shouldSkipFrame(targetFPS: Int): Boolean = {
    val interval = 1000.0 / targetFPS
    val now = js.Date.now()
    val delta = now - lastFrameStarted
    delta < interval
  }

  def startedFrame(targetFPS: Int): Unit = {
    val interval = 1000.0 / targetFPS
    val now = js.Date.now()
    val delta = now - lastFrameStarted
    lastFrameStarted = js.Date.now() - delta % interval
  }

  def updateSmoothedFPS(): Unit = {
    if (context.simulator.timestep % 60 == 0) {
      fps = math.round(60 * 1000 / (js.Date.now() - last60FrameStarted)).toInt
      last60FrameStarted = js.Date.now()
    }
  }
}

