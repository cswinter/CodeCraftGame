package cwinter.codecraft.demos.graphics

import cwinter.codecraft.graphics.engine.{NullPositionDescriptor, ModelDescriptor}
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.Vector2


private[graphics] class MockResource(
  val xPos: Float,
  val yPos: Float,
  val orientation: Float,
  val size: Int
) extends MockObject {

  override def update(): Unit = ()

  override def state(): ModelDescriptor[_] =
    ModelDescriptor(
      NullPositionDescriptor,
      MineralDescriptor(size, xPos, yPos, orientation)
    )

  def dead = false

  def hasVision = false
  def maxSpeed = 0
}
