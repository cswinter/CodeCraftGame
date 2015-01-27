package robowars.simulation

import robowars.worldstate.{RobotObject, WorldObject}

import scala.util.Random


class MockRobot(
  var xPos: Float,
  var yPos: Float,
  var orientation: Float,
  val size: Int
) extends MockObject {
  var targetOrientation = orientation
  val speed = 0.5f
  val turnSpeed = 0.01f

  override def update(): Unit = {
    if (rnd() < 0.003) {
      targetOrientation = (2 * math.Pi * rnd()).toFloat
    }

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


    xPos += vx
    yPos += vy
  }

  override def state(): WorldObject =
    RobotObject(identifier, xPos, yPos, orientation, size)


  def vx = math.cos(orientation).toFloat * speed
  def vy = math.sin(orientation).toFloat * speed



  def rnd() = Random.nextDouble().toFloat


  def dead = false
}
