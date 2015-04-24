package cwinter.graphics.models

import cwinter.graphics.engine.RenderStack
import cwinter.graphics.matrices.{RotationZMatrix4x4, TranslationXYMatrix4x4}
import cwinter.graphics.model.ClosedModel
import cwinter.worldstate._


object TheWorldObjectModelFactory {
  def generateModel(worldObject: WorldObjectDescriptor, timestep: Int)
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
        new RobotModelBuilder(robot, timestep).getModel,
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
      case circle: DrawCircle => new ClosedModel[Unit](
        Unit,
        CircleModelBuilder(circle.radius, circle.identifier).getModel,
        modelview)
      case rectangle: DrawRectangle => new ClosedModel[Unit](
        Unit,
        RectangleModelBuilder(rectangle.bounds).getModel,
        modelview)
    }
  }
}
