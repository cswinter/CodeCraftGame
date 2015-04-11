package robowars.simulation

import cwinter.collisions.{Vector2, CircleLike}
import robowars.worldstate.WorldObject


trait MockObject {
  val identifier = MockObject.genID()

  def update(): Unit
  def state(): WorldObject
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

  implicit object MockObjectIsCircleLike extends CircleLike[MockObject] {
    override def position(t: MockObject): Vector2 = Vector2(t.xPos, t.yPos)
    override def radius(t: MockObject): Float = 0
  }
}
