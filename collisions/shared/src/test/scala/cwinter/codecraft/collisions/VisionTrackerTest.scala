package cwinter.codecraft.collisions

import cwinter.codecraft.util.maths.Vector2
import org.scalatest.FlatSpec

class VisionTrackerTest extends FlatSpec {
  val rotations = (0 to 10).map(_.toDouble) ++ Seq.tabulate(8)(_ * math.Pi / 4)

  "A sine curve" should "see a line at specific intervals" in {
    def sine(t: Double) = Vector2(t, 750 * math.sin((t / 10) * math.Pi))
    def line(t: Double) = Vector2(t, 0)

    for (r <- rotations) {
      val mSine = new RotatedMovingObject(sine, r, "sine")
      val mLine = new RotatedMovingObject(line, r, "line")
      val enters = Map(
        (0, mSine) -> Set(mLine),
        (10, mSine) -> Set(mLine)
      )
      val leaves = Map(
        (1, mSine) -> Set(mLine),
        (11, mSine) -> Set(mLine)
      )
      simulate(Seq(mSine), Seq(mLine), enters, leaves, 15)
    }
  }
  
  "A constant velocity object" should "enter and then leave the sight radius of a stationary object" in {
    def stationary(t: Double) = Vector2(0, 0)
    def moving(t: Double) = Vector2(-19.5 - 20 + t, 0)

    for (r <- rotations) {
      val mStationary = new RotatedMovingObject(stationary, r, "stationary")
      val mMoving = new RotatedMovingObject(moving, r, "moving")
      val enters = Map(
        (20, mStationary) -> Set(mMoving)
      )
      val leaves = Map(
        (60, mStationary) -> Set(mMoving)
      )
      simulate(Seq(mStationary), Seq(mMoving), enters, leaves)
    }
  }
  
  def simulate(
    tracked: Seq[RotatedMovingObject], untracked: Seq[RotatedMovingObject],
    entersEvents: Map[(Int, RotatedMovingObject), Set[RotatedMovingObject]],
    leavesEvents: Map[(Int, RotatedMovingObject), Set[RotatedMovingObject]],
    steps: Int = 100
  ): Unit = {
    val visionTracker = new VisionTracker[RotatedMovingObject](-1000, 1000, -1000, 1000, 20)(MovingObject.MovingObjectIsPositionable)
    for (m <- tracked) visionTracker.insert(m, generateEvents=true)
    for (s <- untracked) visionTracker.insert(s)

    for (t <- 0 to steps) {
      visionTracker.updateAll()

      val events = visionTracker.collectEvents()
      // check that no spurious events are generated
      for ((obj, events) <- events) {
        for (e <- events) e match {
          case visionTracker.EnteredSightRadius(other) =>
            assert(entersEvents((t, obj)).contains(other))
          case visionTracker.LeftSightRadius(other) =>
            assert(leavesEvents((t, obj)).contains(other))
        }
      }
      // check that no events are missing
      for (obj <- tracked) {
        for (nowVisible <- entersEvents.getOrElse((t, obj), Seq())) {
          assert(events.exists(_._2.contains(visionTracker.EnteredSightRadius(nowVisible))))
        }
      }
      for (obj <- tracked) {
        for (nowGone <- leavesEvents.getOrElse((t, obj), Seq())) {
          assert(events.exists(_._2.contains(visionTracker.LeftSightRadius(nowGone))))
        }
      }

      tracked.foreach(_.updatePosition())
      untracked.foreach(_.updatePosition())
    }
  }
}

class MovingObject(pos: Double => Vector2, val name: String = "MovingObject") {
  var t = 0
  private[this] var _position = pos(t)
  def position = _position
  def updatePosition(): Unit = {
    t += 1
    _position = pos(t)
  }
  override def toString = name
}

class RotatedMovingObject(pos: Double => Vector2, angle: Double, name: String = "RotatedMovingObject") extends MovingObject(pos, name) {
  override def position = super.position.rotated(angle)
}

object MovingObject {
  implicit object MovingObjectIsPositionable extends Positionable[MovingObject] {
    override def position(t: MovingObject): Vector2 = t.position
  }
}
