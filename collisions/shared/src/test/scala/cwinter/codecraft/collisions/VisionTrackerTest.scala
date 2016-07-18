package cwinter.codecraft.collisions

import cwinter.codecraft.util.maths.Vector2
import org.scalatest.FlatSpec

class VisionTrackerTest extends FlatSpec {
  val rotations = Seq(0) //(0 to 10).map(_.toDouble) ++ Seq.tabulate(8)(_ * math.Pi / 4)

  "A sine curve" should "see a line at specific intervals" in {
    def sine(t: Double) = Vector2(t, 750 * math.sin((t / 10) * math.Pi))
    def line(t: Double) = Vector2(t, 0)

    for (r <- rotations) {
      val mSine = new RotatedMovingObject(sine, r, "sine") with Tracked
      val mLine = new RotatedMovingObject(line, r, "line") with PassiveVisionTracking
      val expectedEvents = Map[Int, Set[VisionEvent]](
          0 -> Set(EnteredVision(mSine, mLine)),
          1 -> Set(LeftVision(mSine, mLine)),
          10 -> Set(EnteredVision(mSine, mLine)),
          11 -> Set(LeftVision(mSine, mLine))
      )
      simulate(Seq(mSine), Seq(mLine), expectedEvents, 15)
    }
  }

  "A constant velocity object" should "enter and then leave the sight radius of a stationary object" in {
    def stationary(t: Double) = Vector2(0, 0)
    def moving(t: Double) = Vector2(t - 20 - 19.5, 0)

    for (r <- rotations) {
      val mStationary = new RotatedMovingObject(stationary, r, "stationary") with Tracked
      val mMoving = new RotatedMovingObject(moving, r, "moving") with PassiveVisionTracking
      val expectedEvents = Map[Int, Set[VisionEvent]](
          20 -> Set(EnteredVision(mStationary, mMoving)),
          60 -> Set(LeftVision(mStationary, mMoving))
      )
      simulate(Seq(mStationary), Seq(mMoving), expectedEvents)
    }
  }

  def simulate(
      tracked: Seq[RotatedMovingObject with Tracked],
      untracked: Seq[RotatedMovingObject with PassiveVisionTracking],
      expectedEvents: Map[Int, Set[VisionEvent]],
      steps: Int = 100
  ): Unit = {
    val visionTracker =
      new VisionTracker[RotatedMovingObject with VisionTracking](-1000, 1000, -1000, 1000, 20)
    for (m <- tracked) visionTracker.insertActive(m)
    for (s <- untracked) visionTracker.insertPassive(s)

    for (t <- 0 to steps) {
      visionTracker.updateAll(t)

      val actual = tracked.flatMap(_.popEvents()).toSet
      val expected = expectedEvents.getOrElse(t, Set.empty)
      assert(actual === expected)

      tracked.foreach(_.updatePosition())
      untracked.foreach(_.updatePosition())
    }
  }
}

class MovingObject(pos: Double => Vector2, val name: String = "MovingObject") {
  private var t = 0
  private[this] var _position = pos(t)

  def position = _position

  def updatePosition(): Unit = {
    t += 1
    _position = pos(t)
  }

  def maxSpeed: Double = Integer.MAX_VALUE
  override def toString = name
}

class RotatedMovingObject(
    pos: Double => Vector2,
    angle: Double,
    name: String = "RotatedMovingObject"
) extends MovingObject(pos, name) {
  override def position = super.position.rotated(angle)
}

object MovingObject {
  implicit object MovingObjectIsPositionable
      extends Positionable[MovingObject] {
    override def position(t: MovingObject): Vector2 = t.position
  }
}

private[codecraft] trait Tracked extends ActiveVisionTracking { self: MovingObject =>
  private[this] var visibleObjects = Set.empty[VisionTracking]
  private[this] var events = List.empty[VisionEvent]

  def popEvents(): List[VisionEvent] = {
    val result = events
    events = List.empty[VisionEvent]
    result
  }

  def objectEnteredVision(obj: VisionTracking): Unit = {
    events ::= EnteredVision(this, obj)
    visibleObjects += obj
  }
  def objectLeftVision(obj: VisionTracking): Unit = {
    events ::= LeftVision(this, obj)
    visibleObjects -= obj
  }

  def objectRemoved(obj: VisionTracking): Unit =
    if (visibleObjects.contains(obj)) objectRemoved(obj)
}

sealed trait VisionEvent

case class EnteredVision(obj1: Any, obj2: Any) extends VisionEvent

case class LeftVision(obj1: Any, obj2: Any) extends VisionEvent
