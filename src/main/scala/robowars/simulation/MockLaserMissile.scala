package robowars.simulation

import robowars.worldstate.{LaserMissile, WorldObject}

import scala.collection.mutable
import scala.util.Random


class MockLaserMissile(
  var xPos: Float,
  var yPos: Float,
  var orientation: Float
) extends MockObject {
  val speed = 5.0f
  val positions = 15
  val oldPositions = mutable.Queue((xPos, yPos), (xPos, yPos))
  var age = 0

  override def update(): Unit = {
    age += 1
    orientation += 0.1f - 0.2f * rnd()
    xPos += vx
    yPos += vy
    oldPositions.enqueue((xPos, yPos))
    if (oldPositions.length > positions) oldPositions.dequeue()
  }

  override def state(): WorldObject =
    LaserMissile(identifier, oldPositions.toSeq)


  def vx = math.cos(orientation).toFloat * speed
  def vy = math.sin(orientation).toFloat * speed



  def rnd() = Random.nextDouble().toFloat

  def dead = age > 5000
}
