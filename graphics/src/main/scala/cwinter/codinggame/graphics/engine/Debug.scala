package cwinter.codinggame.graphics.engine

import cwinter.codinggame.worldstate.WorldObjectDescriptor


object Debug {
  private[this] var objects = List.empty[WorldObjectDescriptor]
  private[this] var staticObjects = List.empty[WorldObjectDescriptor]

  def draw(worldObject: WorldObjectDescriptor): Unit = {
    objects ::= worldObject
  }

  def drawAlways(worldObject: WorldObjectDescriptor): Unit = {
    staticObjects ::= worldObject
  }


  private[engine] def debugObjects = {
    objects ++ staticObjects
  }

  private[engine] def clear(): Unit =
    objects = List.empty[WorldObjectDescriptor]
}
