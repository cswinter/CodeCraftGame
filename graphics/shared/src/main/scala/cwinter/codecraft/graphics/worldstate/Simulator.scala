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
      case e: Exception =>
        e.printStackTrace()
        paused = true
    }
    t += 1
    savedWorldState = Seq(computeWorldState.toSeq: _*)
  }

  def run(steps: Int): Unit = {
    for (i <- 0 until steps) {
      if (!paused) {
        performUpdate()
      }
    }
  }

  protected def update(): Unit
  def timestep: Int = t
  def togglePause(): Unit = paused = !paused
  def framerateTarget_=(value: Int): Unit = {
    require(value > 0)
    targetFPS = value
  }
  def framerateTarget: Int = targetFPS
  def isPaused: Boolean = paused
  def initialCameraPos: Vector2 = Vector2.Null
  private[codecraft] def worldState: Seq[WorldObjectDescriptor] = savedWorldState
  private[codecraft] def computeWorldState: Iterable[WorldObjectDescriptor]
  private[codecraft] def handleKeypress(keychar: Char): Unit = ()
  private[codecraft] def additionalInfoText: String = ""
}

