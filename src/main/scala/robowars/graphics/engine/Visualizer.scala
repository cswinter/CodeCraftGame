package robowars.graphics.engine

import robowars.graphics.model.{ColorRGB, DrawableModel}
import robowars.graphics.primitives.Polygon
import robowars.worldstate.{MineralObject, WorldObject}
import robowars.graphics.matrices.{RotationZMatrix4x4, TranslationXYMatrix4x4}


class Visualizer(implicit val renderStack: RenderStack) {
  var models = Map.empty[Int, WorldObjectModel]

  def computeModels(worldState: Iterable[WorldObject]): Iterable[DrawableModel] = {
    models = worldState.map { worldObject =>
      if (models.contains(worldObject.identifier))
        (worldObject.identifier, models(worldObject.identifier).update(worldObject))
      else
        (worldObject.identifier, ModelFactory.generateModel(worldObject))
    }.toMap

    models.values.map(_.model)
  }
}


object ModelFactory {
  def generateModel(worldObject: WorldObject)(implicit renderStack: RenderStack): WorldObjectModel = worldObject match {
    case mineral: MineralObject => new MineralObjectModel(mineral)
  }
}


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


class MineralObjectModel(mineral: MineralObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(mineral) {

  val size = mineral.size

  val model =
    new Polygon(5, renderStack.BloomShader)
      .colorMidpoint(ColorRGB(0.03f, 0.6f, 0.03f))
      .colorOutside(ColorRGB(0.0f, 0.1f, 0.0f))
      .scale(math.sqrt(size).toFloat * 20)
      .init()
}
