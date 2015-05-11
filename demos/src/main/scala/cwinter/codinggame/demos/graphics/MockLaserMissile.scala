package cwinter.codinggame.demos.graphics

import cwinter.codinggame.util.maths.Rng
import cwinter.codinggame.worldstate.{RedPlayer, BluePlayer, LaserMissileDescriptor, WorldObjectDescriptor}

import scala.collection.mutable
import scala.util.Random


class MockLaserMissile(
  var xPos: Float,
  var yPos: Float,
  var orientation: Float,
  val lifetime: Int = 45
) extends MockObject {
  val player = if (Rng.bernoulli(0.5f)) BluePlayer else RedPlayer
  val speed = 10.0f
  val positions = 9
  val oldPositions = mutable.Queue((xPos, yPos))
  var age = 0
  var rotationSpeed = 0.0f

  override def update(): Unit = {
    age += 1
    if (rnd() < 0.1) {
      rotationSpeed = 0.1f - 0.2f * rnd()
    }
    orientation += rotationSpeed
    xPos += vx
    yPos += vy
    oldPositions.enqueue((xPos, yPos))
    if (oldPositions.length > positions) oldPositions.dequeue()
    while (oldPositions.length > lifetime - age + 2) oldPositions.dequeue()
  }

  override def state(): WorldObjectDescriptor =
    LaserMissileDescriptor(identifier, oldPositions.toSeq, math.min(positions, age), player)


  def vx = math.cos(orientation).toFloat * speed
  def vy = math.sin(orientation).toFloat * speed



  def rnd() = Random.nextDouble().toFloat

  def dead = age > lifetime
}
