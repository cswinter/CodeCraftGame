package robowars.graphics.engine

import robowars.graphics.matrices.{TranslationXYMatrix4x4, RotationZMatrix4x4}
import robowars.graphics.model._
import robowars.graphics.primitives.{PolygonOutline, Polygon}
import robowars.worldstate.{RobotObject, MineralObject, WorldObject}


class Visualizer(implicit val renderStack: RenderStack) {
  var models = Map.empty[Int, WorldObjectModel]

  def computeModels(worldState: Iterable[WorldObject]): Iterable[DrawableModel] = {
    assert(
      worldState.groupBy(_.identifier).forall(_._2.size == 1),
      "All identifiers must be unique")

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
    case robot: RobotObject => new RobotObjectModel(robot)
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
  val radius = math.sqrt(size).toFloat * 15

  val model =
    new Polygon(5, renderStack.BloomShader)
      .colorMidpoint(ColorRGB(0.03f, 0.6f, 0.03f))
      .colorOutside(ColorRGB(0.0f, 0.1f, 0.0f))
      .scale(radius)
      .zPos(-1)
      .init()
}


class RobotObjectModel(robot: RobotObject)(implicit val rs: RenderStack)
  extends WorldObjectModel(robot) {
  
  val size = robot.size

  val hull =
    new PolygonOutline(renderStack.BloomShader)(5, 40, 47)
      .colorInside(ColorRGB(0.0f, 0.0f, 1f))
      .colorOutside(ColorRGB(0.15f, 0.15f, 1f))

  val body = new Polygon(5, renderStack.MaterialXYRGB)
      .scale(40)
      .color(ColorRGB(0, 0, 0))

  val model = (hull + body).init()
}