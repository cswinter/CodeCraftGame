package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.collisions.Positionable
import cwinter.worldstate.WorldObjectDescriptor


trait WorldObject {
  def position: Vector2

  private[core] def descriptor: WorldObjectDescriptor
  private[core] val id = WorldObject.generateUID()
  private[core] def hasDied: Boolean
}


object WorldObject {
  private[this] var objectCount: Int = -1
  private def generateUID(): Int = {
    objectCount += 1
    objectCount
  }

  implicit object WorldObjectIsPositionable extends Positionable[WorldObject] {
    override def position(t: WorldObject): Vector2 = t.position
  }
}
