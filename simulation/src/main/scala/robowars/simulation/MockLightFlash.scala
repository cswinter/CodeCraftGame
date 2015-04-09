package robowars.simulation

import robowars.worldstate.{LightFlash, WorldObject}


class MockLightFlash(val xPos: Float, val yPos: Float) extends MockObject {
  var stage: Float = 0

  def update(): Unit = stage += 1.0f / 24

  def state(): LightFlash = LightFlash(identifier, xPos, yPos, stage)

  def dead: Boolean = stage > 1
}
