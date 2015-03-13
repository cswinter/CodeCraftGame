package robowars.simulation

import robowars.worldstate.{RobotModule, RobotObject, WorldObject}

import scala.collection.mutable
import scala.util.Random


class MockRobot(
  var xPos: Float,
  var yPos: Float,
  var orientation: Float,
  val size: Int,
  val modules: Seq[RobotModule]
) extends MockObject {
  private[this] var targetOrientation = orientation
  private[this] var stationary = false

  val speed = 2f
  val turnSpeed = 0.01f

  val oldPositions = mutable.Queue((xPos, yPos, orientation), (xPos, yPos, orientation))
  val nPos = 20


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

    // update positions
    if (!stationary) {
      xPos += vx
      yPos += vy
    }

    oldPositions.enqueue((xPos, yPos, orientation))
    if (oldPositions.length > nPos) oldPositions.dequeue()
  }

  override def state(): WorldObject =
    RobotObject(identifier, xPos, yPos, orientation, oldPositions, modules, size)


  def vx = math.cos(orientation).toFloat * speed
  def vy = math.sin(orientation).toFloat * speed



  def rnd() = Random.nextDouble().toFloat


  def dead = false
}
