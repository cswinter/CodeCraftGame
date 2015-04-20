package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.matrices.{RotationZMatrix4x4, TranslationXYMatrix4x4}
import robowars.graphics.model.ClosedModel
import robowars.worldstate._


object TheWorldObjectModelFactory {
  def generateModel(worldObject: WorldObjectDescriptor)
      (implicit renderStack: RenderStack): ClosedModel[_] = {

    val xPos = worldObject.xPos
    val yPos = worldObject.yPos
    val orientation = worldObject.orientation
    val modelview =
      new RotationZMatrix4x4(orientation) *
        new TranslationXYMatrix4x4(xPos, yPos)

    worldObject match {
      case mineral: MineralDescriptor => new ClosedModel[Unit](
        Unit,
        new MineralModelBuilder(mineral).getModel,
        modelview)
      case robot: DroneDescriptor => new ClosedModel(
        robot,
        new RobotModelBuilder(robot).getModel,
        modelview)
      case lightFlash: LightFlashDescriptor => new ClosedModel(
        lightFlash,
        new LightFlashModelBuilder(lightFlash).getModel,
        modelview)
      case laserMissile: LaserMissileDescriptor => new ClosedModel[Unit](
        Unit,
        LaserMissileModelFactory.build(laserMissile.positions),
        modelview)
      case TestingObject(t) => new ClosedModel[Unit](
        Unit,
        new TestModelBuilder(t).getModel,
        modelview)
      case circle: Circle => new ClosedModel[Unit](
        Unit,
        CircleModelBuilder(circle.radius, circle.identifier).getModel,
        modelview)
      case rectangle: Rectangle => new ClosedModel[Unit](
        Unit,
        RectangleModelBuilder(rectangle.bounds).getModel,
        modelview)
    }
  }
}
