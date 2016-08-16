package cwinter.codecraft.graphics.engine

import cwinter.codecraft.util.maths.Vector2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


private[codecraft] trait Simulator {
  @volatile private[this] var savedWorldState = Seq.empty[ModelDescriptor[_]]

  @volatile private[this] var running = false
  private[this] var paused = false
  private[this] var tFrameCompleted = System.nanoTime()
  private[this] var targetFPS = 60
  @volatile private[this] var t = -1
  private[this] def frameMillis = 1000.0 / targetFPS
  private[this] var stopped = false
  private[this] var exceptionHandler: Option[Throwable => _] = None
  private[this] var _measuredFramerate: Int = 0
  private[this] var _nanoTimeLastMeasurement: Long = 0
  private var currentlyUpdating = false
  protected[codecraft] var debug = new Debug
  var graphicsEnabled: Boolean = true

  /** Runs the game until the program is terminated. */
  def run(): Unit = synchronized {
    require(!running, "Simulator.run() must only be called once.")
    running = true
    Future {
      while (!stopped && gameStatus == Running) {
        if (!paused) {
          performUpdate()
        }

        val nanos = System.nanoTime()
        val dt = nanos - tFrameCompleted
        val sleepMillis = frameMillis - dt / 1000000
        if (sleepMillis > 0) {
          Thread.sleep(sleepMillis.toInt)
        }
        tFrameCompleted = System.nanoTime()
      }
    }
  }

  private def performUpdate(): Unit = {
    recomputeGraphicsState()
    t += 1
    try {
      measureFPS()
      update()
    } catch {
      case e: Throwable =>
        exceptionHandler.foreach(_(e))
        if (stopped) return
        e.printStackTrace()
        paused = true
    }
    debug.swapBuffers()
  }

  private[codecraft] def performAsyncUpdate(): Future[Unit] = {
    assert(!currentlyUpdating)
    currentlyUpdating = true
    recomputeGraphicsState()
    t += 1
    measureFPS()
    val updateFuture = asyncUpdate()

    // optimization that prevents JavaScript from sometimes skipping an animation frame (in single player)
    if (updateFuture.isCompleted) currentlyUpdating = false

    updateFuture.onComplete {
      case Success(_) =>
        currentlyUpdating = false
        debug.swapBuffers()
      case Failure(e) =>
        exceptionHandler.foreach(_(e))
        if (stopped) return Future.successful(Unit)
        e.printStackTrace()
        paused = true
        currentlyUpdating = false
        debug.swapBuffers()
    }
    updateFuture
  }

  /** Will run the game for `steps` timesteps. */
  def run(steps: Int): Unit = {
    for (i <- 0 until steps) {
      if (!paused) {
        performUpdate()
        if (stopped || gameStatus != Running) return
      }
    }
  }

  private def measureFPS(): Unit = {
    if (timestep % 60 == 0 && !isPaused) {
      val nanoTimeNow = System.nanoTime()
      _measuredFramerate = (60 * 1000 * 1000 * 1000L / (nanoTimeNow - _nanoTimeLastMeasurement)).toInt
      _nanoTimeLastMeasurement = nanoTimeNow
    }
  }

  private def recomputeGraphicsState(): Unit = {
    if (gameStatus == Running && graphicsEnabled) {
      savedWorldState = Seq(computeWorldState.toSeq: _*)
    }
  }

  /** Performs one timestep. */
  protected def update(): Unit

  /** Asynchronously performs one timestep.
    * Returns a future which completes once all changes have taken effect.
    */
  protected def asyncUpdate(): Future[Unit]

  /** Returns the game's status
    */
  protected def gameStatus: Status

  /** Returns the current timestep. */
  def timestep: Int = t

  /** Pauses or resumes the game as applicable. */
  def togglePause(): Unit = paused = !paused

  /** Sets the target framerate to the given value.
    *
    * @param value The new framerate target.
    */
  def framerateTarget_=(value: Int): Unit = {
    require(value > 0)
    targetFPS = value
  }

  /** Returns the target framerate in frames per second. */
  def framerateTarget: Int = targetFPS

  /** Returns the number of ticks per second measure over the last 60 tick interval. */
  def measuredFramerate: Int = _measuredFramerate

  /** Returns true if the game is currently paused. */
  def isPaused: Boolean = paused

  /** Returns the initial camera position in the game world. */
  def initialCameraPos: Vector2 = Vector2.Null

  /** Terminates any running game loops. */
  def terminate(): Unit = stopped = true

  private[codecraft] def isCurrentlyUpdating: Boolean = currentlyUpdating

  private[codecraft] def onException(callback: Throwable => _): Unit = {
    exceptionHandler = Some(callback)
  }

  private[codecraft] def worldState: Seq[ModelDescriptor[_]] = debug.debugObjects ++ savedWorldState
  private[codecraft] def computeWorldState: Iterable[ModelDescriptor[_]]
  private[codecraft] def handleKeypress(keychar: Char): Unit = ()
  private[codecraft] def additionalInfoText: String = ""
  private[codecraft] def textModels: Iterable[TextModel] = debug.textModels


  protected sealed trait Status
  protected case object Running extends Status
  protected case class Stopped(reason: String) extends Status
  protected case class Crashed(exception: Throwable) extends Status

  def forceGL2: Boolean = false
}

