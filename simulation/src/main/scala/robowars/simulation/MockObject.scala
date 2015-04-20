package robowars.simulation

import cwinter.codinggame.maths.Vector2
import cwinter.collisions.Positionable
import robowars.worldstate.WorldObjectDescriptor


trait MockObject {
  val identifier = MockObject.genID()

  def update(): Unit
  def state(): WorldObjectDescriptor
  def dead: Boolean

  def xPos: Float
  def yPos: Float
}

object MockObject {
  private var objectCount = 0
  private def genID(): Int = {
    objectCount += 1
    objectCount
  }

  implicit object MockObjectIsPositionable extends Positionable[MockObject] {
    override def position(t: MockObject): Vector2 = Vector2(t.xPos, t.yPos)
  }
}
