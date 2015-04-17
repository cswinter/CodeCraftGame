package robowars.graphics.engine

import robowars.worldstate.WorldObject


object Debug {
  private[this] var objects = List.empty[WorldObject]
  private[this] var staticObjects = List.empty[WorldObject]

  def draw(worldObject: WorldObject): Unit = {
    objects ::= worldObject
  }

  def drawAlways(worldObject: WorldObject): Unit = {
    staticObjects ::= worldObject
  }


  private[engine] def debugObjects = {
    objects ++ staticObjects
  }

  private[engine] def clear(): Unit =
    objects = List.empty[WorldObject]
}
