package cwinter.codecraft.graphics.worldstate

import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.util.maths.Vector2

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

private[codecraft] trait Simulator {
  @volatile private[this] var savedWorldState = Seq.empty[WorldObjectDescriptor]

  @volatile private[this] var running = false
  private[this] var paused = false
  private[this] var tFrameCompleted = System.nanoTime()
  private[this] var targetFPS = 30
  @volatile private[this] var t = 0
  private[this] def frameMillis = 1000 / targetFPS

  /**
   * Runs the game until the program is terminated.
   */
  def run(): Unit = synchronized {
    require(!running, "Simulator.run() must only be called once.")
    running = true
    Future {
      while (true) {
        if (!paused) {
          performUpdate()
        }

        val nanos = System.nanoTime()
        val dt = nanos - tFrameCompleted
        tFrameCompleted = nanos
        val sleepMillis = frameMillis - dt / 1000000
        if (sleepMillis > 0) {
          Thread.sleep(sleepMillis)
        }
      }
    }
  }

  private def performUpdate(): Unit = {
    Debug.clear()
    try {
      update()
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        paused = true
    }
    t += 1
    savedWorldState = Seq(computeWorldState.toSeq: _*)
  }

  /**
   * Will run the game for `steps` timesteps.
   */
  def run(steps: Int): Unit = {
    for (i <- 0 until steps) {
      if (!paused) {
        performUpdate()
      }
    }
  }

  /**
   * Performs one timestep.
   */
  protected def update(): Unit

  /**
    * Asynchronously performs one timestep.
    * Returns a future which completes once all changes have taken effect.
    */
  protected def asyncUpdate(): Future[Unit]

  /**
   * Returns the current timestep.
   */
  def timestep: Int = t

  /**
   * Pauses or resumes the game as applicable.
   */
  def togglePause(): Unit = paused = !paused

  /**
   * Sets the target framerate to the given value.
   * @param value The new framerate target.
   */
  def framerateTarget_=(value: Int): Unit = {
    require(value > 0)
    targetFPS = value
  }

  /**
   * Returns the target framerate in frames per second.
   */
  def framerateTarget: Int = targetFPS

  /**
   * Returns true if the game is currently paused.
   */
  def isPaused: Boolean = paused

  /**
   * Returns the initial camera position in the game world.
   */
  def initialCameraPos: Vector2 = Vector2.Null

  private[codecraft] def worldState: Seq[WorldObjectDescriptor] = savedWorldState
  private[codecraft] def computeWorldState: Iterable[WorldObjectDescriptor]
  private[codecraft] def handleKeypress(keychar: Char): Unit = ()
  private[codecraft] def additionalInfoText: String = ""
}

