package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor
import cwinter.codecraft.util.maths.ColorRGBA


object Debug {
  private[this] var objects = List.empty[WorldObjectDescriptor]
  private[this] var staticObjects = List.empty[WorldObjectDescriptor]
  private[this] var _textModels = List.empty[TextModel]

  def draw(worldObject: WorldObjectDescriptor): Unit = {
    objects ::= worldObject
  }

  def drawAlways(worldObject: WorldObjectDescriptor): Unit = {
    staticObjects ::= worldObject
  }

  def drawText(text: String, xPos: Double, yPos: Double, color: ColorRGBA): Unit = {
    _textModels ::= TextModel(text, xPos.toFloat, yPos.toFloat, color)
  }

  private[engine] def debugObjects = {
    objects ++ staticObjects
  }

  private[engine] def textModels = _textModels

  private[codecraft] def clear(): Unit = {
    objects = List.empty[WorldObjectDescriptor]
    _textModels = List.empty[TextModel]
  }
}
