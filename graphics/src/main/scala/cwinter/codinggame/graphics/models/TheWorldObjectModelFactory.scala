package cwinter.codinggame.graphics.models

import cwinter.codinggame.graphics.engine.RenderStack
import cwinter.codinggame.graphics.matrices.{IdentityMatrix4x4, RotationZMatrix4x4, TranslationXYMatrix4x4}
import cwinter.codinggame.graphics.model.ClosedModel
import cwinter.codinggame.util.maths.VertexXY
import cwinter.codinggame.worldstate._


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
        new DroneModelBuilder(robot, timestep).getModel,
        modelview)
      case lightFlash: LightFlashDescriptor => new ClosedModel(
        lightFlash,
        new LightFlashModelBuilder(lightFlash).getModel,
        modelview)
      case LaserMissileDescriptor(_, positions, maxPos, player) => new ClosedModel[Unit](
        Unit,
        LaserMissileModelFactory.build(positions, maxPos, player),
        modelview)
      case ManipulatorArm(player, x1, y1, x2, y2) => new ClosedModel[Unit](
        Unit,
        ManipulatorArmModelFactory.build(player, x1, y1, x2, y2),
        IdentityMatrix4x4
      )
      case EnergyGlobeDescriptor(x, y) => new ClosedModel[Unit](
        Unit,
        EnergyGlobeModelFactory.build(VertexXY(x, y)).noCaching.getModel,
        IdentityMatrix4x4
      )
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
