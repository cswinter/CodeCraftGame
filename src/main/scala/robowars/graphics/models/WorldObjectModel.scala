package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.matrices.{RotationZMatrix4x4, TranslationXYMatrix4x4}
import robowars.graphics.model.DrawableModel
import robowars.worldstate.WorldObject


abstract class WorldObjectModel(worldObject: WorldObject)(implicit val renderStack: RenderStack) {
  val identifier = worldObject.identifier
  protected var xPos = worldObject.xPos
  protected var yPos = worldObject.yPos
  protected var orientation = worldObject.orientation

  def update(worldObject: WorldObject): this.type = {
    xPos = worldObject.xPos
    yPos = worldObject.yPos
    orientation = worldObject.orientation

    val modelview =
      new RotationZMatrix4x4(orientation) *
        new TranslationXYMatrix4x4(xPos, yPos)

    model.setModelview(modelview)

    this
  }


  def model: DrawableModel
}
