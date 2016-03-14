package cwinter.codecraft.graphics.engine

import cwinter.codecraft.util.maths.ColorRGBA

private[graphics] case class TextModel(
  text: String,
  xPos: Float,
  yPos: Float,
  color: ColorRGBA,
  absolutePos: Boolean = false,
  largeFont: Boolean = false
)

