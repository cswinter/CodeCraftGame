package robowars.simulation

import robowars.worldstate.{MineralObject, WorldObject}


class MockResource(
  val xPos: Float,
  val yPos: Float,
  val orientation: Float,
  val size: Int
) extends MockObject {

  override def update(): Unit = ()

  override def state(): WorldObject =
    MineralObject(identifier, xPos, yPos, orientation, size)

  def dead = false
}
