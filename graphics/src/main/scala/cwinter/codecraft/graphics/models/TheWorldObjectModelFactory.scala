package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.matrices.{IdentityMatrix4x4, RotationZMatrix4x4, TranslationXYMatrix4x4}
import cwinter.codecraft.graphics.model.{PolygonRing, ClosedModel}
import cwinter.codecraft.util.maths.VertexXY
import cwinter.codecraft.worldstate._


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
      case drone: DroneDescriptor => new ClosedModel(
        drone,
        new DroneModelBuilder(drone, timestep).getModel,
        modelview)
      case lightFlash: LightFlashDescriptor => new ClosedModel(
        lightFlash,
        new LightFlashModelBuilder(lightFlash).getModel,
        modelview)
      case HomingMissileDescriptor(_, positions, maxPos, player) => new ClosedModel[Unit](
        Unit,
        HomingMissileModelFactory.build(positions, maxPos, player),
        modelview)
      case ManipulatorArm(player, x1, y1, x2, y2) => new ClosedModel[Unit](
        Unit,
        ManipulatorArmModelFactory.build(player, x1, y1, x2, y2),
        IdentityMatrix4x4
      )
      case EnergyGlobeDescriptor(x, y, f) => new ClosedModel[Unit](
        Unit,
        EnergyGlobeModelFactory.build(VertexXY(x, y), f).noCaching.getModel,
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
      case DrawCircleOutline(x, y, radius, color) => new ClosedModel[Unit](
        Unit,
        new PolygonRing(
          renderStack.MaterialXYRGB, 40, Seq.fill(40)(color), Seq.fill(40)(color),
          radius - 2, radius, VertexXY(0, 0), 0, 0
        ).noCaching.getModel,
        modelview)
      case rectangle: DrawRectangle => new ClosedModel[Unit](
        Unit,
        RectangleModelBuilder(rectangle.bounds).getModel,
        modelview)
    }
  }
}
