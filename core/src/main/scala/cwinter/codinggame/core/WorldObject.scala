package cwinter.codinggame.core

import cwinter.codinggame.maths.Vector2
import robowars.worldstate.WorldObjectDescriptor


trait WorldObject {
  def position: Vector2
  
  private[core] def descriptor: WorldObjectDescriptor
  val id = WorldObject.generateUID()
}


object WorldObject {
  private[this] var objectCount: Int = -1
  private def generateUID(): Int = {
    objectCount += 1
    objectCount
  }
}
