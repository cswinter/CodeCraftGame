package robowars.graphics.engine

import robowars.graphics.materials.Intensity
import robowars.graphics.matrices.{DilationXYMatrix4x4, DilationMatrix4x4, TranslationXYMatrix4x4, RotationZMatrix4x4}
import robowars.graphics.model._
import robowars.graphics.primitives.{CircleSegment, Square, PolygonOutline, Polygon}
import robowars.worldstate.{LightFlash, RobotObject, MineralObject, WorldObject}


class Visualizer(implicit val renderStack: RenderStack) {
  var models = Map.empty[Int, WorldObjectModel]

  def computeModels(worldState: Iterable[WorldObject]): Iterable[DrawableModel] = {
    assert(
      worldState.groupBy(_.identifier).forall(_._2.size == 1),
      "All identifiers must be unique")

    models = worldState.map { worldObject =>
      if (models.contains(worldObject.identifier))
        (worldObject.identifier, models(worldObject.identifier).update(worldObject))
      else {
        val newModel = ModelFactory.generateModel(worldObject)
        newModel.update(worldObject)
        (worldObject.identifier, newModel)
      }
    }.toMap

    models.values.map(_.model)
  }
}


object ModelFactory {
  def generateModel(worldObject: WorldObject)(implicit renderStack: RenderStack): WorldObjectModel = worldObject match {
    case mineral: MineralObject => new MineralObjectModel(mineral)
    case robot: RobotObject => new RobotObjectModel(robot)
    case lightFlash: LightFlash => new LightFlashObjectModel(lightFlash)
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

  val radius = 30
  val hull =
    new PolygonOutline(renderStack.BloomShader)(5, radius, radius + 6)
      .colorInside(ColorRGB(0.15f, 0.15f, 1f))
      .colorOutside(ColorRGB(0.0f, 0.0f, 1f))

  val body = new Polygon(5, renderStack.MaterialXYRGB)
      .scale(radius)
      .color(ColorRGB(0, 0, 0))

  val sideLength = 2 * (radius + 6) * math.sin(math.Pi / 5).toFloat
  val innerRadius = (radius + 6) * math.cos(math.Pi / 5).toFloat

  val booster1 = new CircleSegment(6, 0.7f, renderStack.BloomShader)
    .color(ColorRGB(0.95f, 0.95f, 0.95f))
    .scaleX(6)
    .scaleY(sideLength / 5)
    .translate(-innerRadius, sideLength / 4)

  val booster2 = new CircleSegment(6, 0.7f, renderStack.BloomShader)
    .color(ColorRGB(0.95f, 0.95f, 0.95f))
    .scaleX(6)
    .scaleY(sideLength / 5)
    .translate(-innerRadius, -sideLength / 4)

  val module1Hull = new PolygonOutline(renderStack.BloomShader)(6, 6, 10)
    .color(ColorRGB(0.0f, 0.0f, 1f))
    //.rotate(2 * math.Pi.toFloat / 10)
    .translate(innerRadius - 3, 0)
    .rotate(2 * math.Pi.toFloat / 10)
    .zPos(-1)

  val weapon1 = new Polygon(6, renderStack.MaterialXYRGB)
    .colorMidpoint(ColorRGB(0.5f, 0.5f, 1))
    .colorOutside(ColorRGB(0, 0, 0))
    //.rotate(2 * math.Pi.toFloat / 10)
    .scale(6)
    .translate(innerRadius - 3, 0)
    .rotate(2 * math.Pi.toFloat / 10)
    .zPos(1)


  val module2Hull = new PolygonOutline(renderStack.BloomShader)(6, 6, 10)
    .color(ColorRGB(0.0f, 0.0f, 1f))
    //.rotate(2 * math.Pi.toFloat / 10)
    .translate(innerRadius - 3, 0)
    .rotate(-2 * math.Pi.toFloat / 10)
    .zPos(-1)

  val weapon2 = new Polygon(6, renderStack.BloomShader)
    .colorMidpoint(ColorRGB(1, 0.5f, 0))
    .colorOutside(ColorRGB(0.5f, 0, 0))
    //.rotate(2 * math.Pi.toFloat / 10)
    .scale(6)
    .translate(innerRadius - 3, 0)
    .rotate(-2 * math.Pi.toFloat / 10)
    .zPos(2)

  /*val blah = new CircleSegment(6, 10, 1, renderStack.BloomShader)
    .color(ColorRGB(1, 1, 1))
    .scale(100)
    .zPos(1)*/

  val model = (hull + body + booster1 + booster2 + module1Hull + weapon1 + module2Hull + weapon2).init()
}


class LightFlashObjectModel(mineral: LightFlash)(implicit val rs: RenderStack)
  extends WorldObjectModel(mineral) {

  val lightFlashModel =
    new Polygon(25, renderStack.GaussianGlowPIntensity)
      .colorMidpoint(ColorRGBA(1, 1, 1, 0))
      .colorOutside(ColorRGBA(1, 1, 1, 1))
      .scale(1)
      .zPos(-1)
      .initParameterized(renderStack.GaussianGlowPIntensity)

  val model = lightFlashModel

  override def update(worldObject: WorldObject): this.type = {
    super.update(worldObject)

    val lightFlash = worldObject.asInstanceOf[LightFlash]

    val modelview = new DilationXYMatrix4x4(60 * lightFlash.stage + 5) * model.modelview
    model.setModelview(modelview)

    lightFlashModel.params = Intensity(1 - lightFlash.stage)

    this
  }
}
