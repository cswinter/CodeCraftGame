package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Vector2
import cwinter.worldstate.{WorldObjectDescriptor, LightFlashDescriptor}


class LightFlash(val xPos: Float, val yPos: Float) extends WorldObject {
  var stage: Float = 0

  override def position: Vector2 = Vector2(xPos, yPos)

  override private[core] def descriptor: WorldObjectDescriptor = LightFlashDescriptor(id, xPos, yPos, stage)

  def update(): Unit = stage += 1.0f / 24

  def hasDied: Boolean = stage > 1
}
