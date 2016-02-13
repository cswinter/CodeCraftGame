package cwinter.codecraft.demos.graphics

import cwinter.codecraft.collisions.Positionable
import cwinter.codecraft.graphics.worldstate.{ModelDescriptor, WorldObjectDescriptor}
import cwinter.codecraft.util.maths.Vector2


private[graphics] trait MockObject {
  val identifier = MockObject.genID()

  def update(): Unit
  def state(): ModelDescriptor
  def dead: Boolean

  def xPos: Float
  def yPos: Float
}

private[graphics] object MockObject {
  private var objectCount = 0
  private def genID(): Int = {
    objectCount += 1
    objectCount
  }

  implicit object MockObjectIsPositionable extends Positionable[MockObject] {
    override def position(t: MockObject): Vector2 = Vector2(t.xPos, t.yPos)
  }
}
