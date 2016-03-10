package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.worldstate.{ModelDescriptor, WorldObjectDescriptor}
import cwinter.codecraft.util.maths.{Vector2, ColorRGBA}


object Debug {
  private[this] var objects = List.empty[ModelDescriptor]
  private[this] var staticObjects = List.empty[ModelDescriptor]
  private[this] var _textModels = List.empty[TextModel]

  private[codecraft] def draw(worldObject: ModelDescriptor): Unit = {
    objects ::= worldObject
  }

  private[codecraft] def drawAlways(worldObject: ModelDescriptor): Unit = {
    staticObjects ::= worldObject
  }

  def drawText(
    text: String, xPos: Double, yPos: Double, color: ColorRGBA,
    absolutePosition: Boolean = false, largeFont: Boolean = false
  ): Unit = {
    _textModels ::= TextModel(text, xPos.toFloat, yPos.toFloat, color, absolutePosition, largeFont, absolutePosition)
  }


  private[this] var _cameraOverride: Option[() => Vector2] = None
  def setCameraOverride(getPos: => Vector2): Unit = {
    _cameraOverride = Some(() => getPos)
  }

  def cameraOverride: Option[Vector2] = _cameraOverride.map(_())

  private[engine] def debugObjects = {
    objects ++ staticObjects
  }

  private[engine] def textModels = _textModels

  private[codecraft] def clear(): Unit = {
    objects = List.empty[ModelDescriptor]
    _textModels = List.empty[TextModel]
  }

  private[cwinter] def clearDrawAlways(): Unit = {
    staticObjects = List.empty[ModelDescriptor]
  }
}
