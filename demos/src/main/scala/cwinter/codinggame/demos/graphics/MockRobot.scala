package cwinter.codinggame.demos.graphics

import cwinter.worldstate._

import scala.collection.mutable
import scala.util.Random


class MockRobot(
  var xPos: Float,
  var yPos: Float,
  var orientation: Float,
  val size: Int,
  var modules: Seq[DroneModule],
  val sightRadius: Option[Int]
) extends MockObject {
  private[this] var targetOrientation = orientation
  private[this] var stationary = false
  private[this] val hullState =
    if (rnd() < 0.8) Seq.fill(size - 1)(2.toByte)
    else Seq.fill(size - 1)(Random.nextInt(3).toByte)

  var inSight = Set.empty[MockObject]


  val speed = 2f
  val turnSpeed = 0.01f

  val oldPositions = mutable.Queue((xPos, yPos, orientation), (xPos, yPos, orientation))
  val nPos = 12



  override def update(): Unit = {
    // randomly choose new target orientation
    if (rnd() < 0.003) {
      val LimitX = 1500
      val LimitY = 1000
      if (xPos > LimitX) {
        targetOrientation = math.Pi.toFloat
      } else if (xPos < -LimitX) {
        targetOrientation = 0
      } else if (yPos > LimitY) {
        targetOrientation = 3 * math.Pi.toFloat / 2
      } else if (yPos < -LimitY) {
        targetOrientation = math.Pi.toFloat / 2
      } else {
        targetOrientation = (2 * math.Pi * rnd()).toFloat
      }
    }

    // adjust orientation towards target orientation
    if (targetOrientation != orientation) {
      val diff = targetOrientation - orientation
      val diffP = if (diff < 0) diff + 2 * math.Pi else diff
      if (diffP <= turnSpeed) {
        orientation = targetOrientation
      } else if (diffP < math.Pi) {
        orientation += turnSpeed
      } else {
        orientation -= turnSpeed
      }

      if (orientation < 0) orientation += 2 * math.Pi.toFloat
      if (orientation > 2 * math.Pi) orientation -= 2 * math.Pi.toFloat
    }

    if (stationary && rnd() < 0.01 || !stationary && rnd() < 0.001) {
      stationary = !stationary
    }

    // update timer on engines
    modules = modules.map {
      case ProcessingModule(pos, t) =>
        ProcessingModule(pos, t.map(x => (x + 1) % 250))
      case StorageModule(positions, rc, t) =>
        StorageModule(
          positions,
          rc,
          t.map(x => (x + 1) % 250)
        )
      case m => m
    }

    // update positions
    if (!stationary) {
      xPos += vx
      yPos += vy
    }

    oldPositions.enqueue((xPos, yPos, orientation))
    if (oldPositions.length > nPos) oldPositions.dequeue()
  }

  override def state(): WorldObjectDescriptor =
    DroneDescriptor(
      identifier,
      xPos, yPos,
      orientation,
      oldPositions,
      modules,
      hullState,
      size,

      BluePlayer,
      None,

      sightRadius,
      Some(inSight.map(obj => (obj.xPos, obj.yPos)))
    )


  def vx = math.cos(orientation).toFloat * speed
  def vy = math.sin(orientation).toFloat * speed



  def rnd() = Random.nextDouble().toFloat


  def dead = false
}
