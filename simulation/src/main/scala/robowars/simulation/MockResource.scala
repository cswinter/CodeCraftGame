package robowars.simulation

import robowars.worldstate.{MineralDescriptor, WorldObjectDescriptor}


class MockResource(
  val xPos: Float,
  val yPos: Float,
  val orientation: Float,
  val size: Int
) extends MockObject {

  override def update(): Unit = ()

  override def state(): WorldObjectDescriptor =
    MineralDescriptor(identifier, xPos, yPos, orientation, size)

  def dead = false
}
