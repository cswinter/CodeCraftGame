package cwinter.codecraft.demos.graphics

import cwinter.codecraft.graphics.worldstate.{ModelDescriptor, LightFlashDescriptor}


private[graphics] class MockLightFlash(val xPos: Float, val yPos: Float) extends MockObject {
  var stage: Float = 0

  def update(): Unit = stage += 1.0f / 24

  def state(): ModelDescriptor = ModelDescriptor(xPos, yPos, 0, LightFlashDescriptor(stage))

  def dead: Boolean = stage > 1
}
