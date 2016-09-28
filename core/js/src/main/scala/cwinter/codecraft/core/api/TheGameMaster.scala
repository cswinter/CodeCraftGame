package cwinter.codecraft.core.api

import cwinter.codecraft.core.game.DroneWorldSimulator
import cwinter.codecraft.graphics.engine.WebGLRenderer
import cwinter.codecraft.util.maths.ColorRGBA
import org.scalajs.dom
import org.scalajs.dom.html

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
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
  private[this] var runContext: Option[RunContext] = None
  private[codecraft] var outputFPS: Boolean = false


  def run(simulator: DroneWorldSimulator): DroneWorldSimulator = {
    require(canvas != null, "Must first set TheGameMaster.canvas variable to the webgl canvas element.")
    require(runContext.isEmpty, "Can only run one CodeCraft game at a time.")

    val lockstepGraphics = !simulator.precomputeFrames
    val renderer = new WebGLRenderer(canvas, simulator)
    val context = new RunContext(simulator, renderer, 16, lockstepGraphics)
    runContext = Some(context)
    runGraphics(context)
    if (!lockstepGraphics) runGame(context)
    simulator
  }


  private def runGraphics(context: RunContext): Unit = {
    import context._
    if (stopped) return
    dom.window.requestAnimationFrame((d: Double) => runGraphics(context))

    if (!lockstepGraphics || !fps.shouldSkipFrame(simulator.framerateTarget)) {
      fps.startedFrame(simulator.framerateTarget)

      fps.updateSmoothedFPS()
      if (outputFPS && !simulator.isPaused) {
        fps.drawFPS()
        if (simulator.timestep % 100 == 0) fps.printFPS()
      }
      renderer.render()
    }

    if (lockstepGraphics && !simulator.isPaused && !simulator.isCurrentlyUpdating)
      simulator.performAsyncUpdate()
  }

  private[codecraft] def runGame(context: RunContext): Unit = {
    import context._
    if (simulator.stopped || simulator.gameStatus != simulator.Running) return

    if (simulator.isPaused) scala.scalajs.js.timers.setTimeout(20.0)(runGame(context))
    else {
      if (outputFPS && !simulator.isPaused) {
        fps.drawFPS()
        if (simulator.timestep % 100 == 0) fps.printFPS()
      }
      simulator.performAsyncUpdate().onComplete {
        case Success(_) =>
          simulator.excessMillis match {
            case (Some(time), _) =>
              scala.scalajs.js.timers.setTimeout(time) {
                simulator.tFrameCompleted = System.nanoTime()
                runGame(context)
              }
            case (None, resetTime) =>
              if (resetTime) simulator.tFrameCompleted = System.nanoTime()
              runGame(context)
          }
        case Failure(x) => x.printStackTrace()
      }
    }
  }

  def stop(): Unit = {
    runContext.foreach(_.stop())
    runContext = None
  }

  def currentFPS: Option[Int] = runContext.map(_.fps.fps)
}

class RunContext(
  val simulator: DroneWorldSimulator,
  val renderer: WebGLRenderer,
  val targetMillisPerFrame: Int,
  val lockstepGraphics: Boolean,
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
      s"FPS: ${context.simulator.measuredFramerate}"
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

