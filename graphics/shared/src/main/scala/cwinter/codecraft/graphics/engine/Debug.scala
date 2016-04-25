package cwinter.codecraft.graphics.engine

import cwinter.codecraft.util.maths.{ColorRGBA, Vector2}


object Debug {
  private[this] var objects = List.empty[ModelDescriptor[_]]
  private[this] var _textModels = List.empty[TextModel]

  private[codecraft] def draw(worldObject: ModelDescriptor[_]): Unit = {
    objects ::= worldObject
  }

  private[codecraft] def drawText(
    text: String, xPos: Double, yPos: Double, color: ColorRGBA,
    absolutePosition: Boolean, centered: Boolean, largeFont: Boolean
  ): Unit = _textModels ::= TextModel(text, xPos.toFloat, yPos.toFloat, color, absolutePosition, centered, largeFont)

  def drawText(text: String, xPos: Double, yPos: Double, color: ColorRGBA): Unit =
    drawText(text, xPos, yPos, color, absolutePosition=false, centered = true, largeFont=false)


  private[this] var _cameraOverride: Option[() => Vector2] = None
  def setCameraOverride(getPos: => Vector2): Unit = {
    _cameraOverride = Some(() => getPos)
  }

  def cameraOverride: Option[Vector2] = _cameraOverride.map(_())

  private[engine] def debugObjects = {
    objects
  }

  private[engine] def textModels = _textModels

  private[codecraft] def clear(): Unit = {
    objects = List.empty[ModelDescriptor[_]]
    _textModels = List.empty[TextModel]
  }
}
