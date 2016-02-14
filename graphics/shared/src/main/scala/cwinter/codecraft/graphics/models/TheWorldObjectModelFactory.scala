package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.util.maths.matrices._
import cwinter.codecraft.graphics.model.{Model, PolygonRing, ClosedModel}
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.VertexXY


private[graphics] object TheWorldObjectModelFactory {
  def generateModel(modelDescriptor: ModelDescriptor, timestep: Int)
      (implicit renderStack: RenderStack): ClosedModel[_] = {

    val modelview = obtainModelview(modelDescriptor.position)

    modelDescriptor.objectDescriptor match {
      case mineral: MineralDescriptor => new ClosedModel[Unit](
        Unit,
        new MineralModelBuilder(mineral).getModel,
        modelview)
      case drone: DroneDescriptor =>
        if (modelDescriptor.objectDescriptor.cachedModel.isEmpty) {
          modelDescriptor.objectDescriptor.cachedModel = new DroneModelBuilder(drone, timestep).getModel
        }
        new ClosedModel(
          drone,
          modelDescriptor.objectDescriptor.cachedModel.get.asInstanceOf[Model[DroneDescriptor]],
          modelview
        )
      case lightFlash: LightFlashDescriptor => new ClosedModel(
        lightFlash,
        new LightFlashModelBuilder(lightFlash).getModel,
        modelview)
      case HomingMissileDescriptor(positions, maxPos, player) => new ClosedModel[Unit](
        Unit,
        HomingMissileModelFactory.build(positions, maxPos, player),
        modelview)
      case energyGlobe: EnergyGlobeDescriptor => new ClosedModel[Unit](
        Unit,
        new EnergyGlobeModelBuilder(energyGlobe).getModel,
        modelview
      )
      case TestingObject(t) => new ClosedModel[Unit](
        Unit,
        new TestModelBuilder(t).getModel,
        modelview)
      case circle: DrawCircle => new ClosedModel[Unit](
        Unit,
        CircleModelBuilder(circle.radius, circle.identifier).getModel,
        modelview)
      case DrawCircleOutline(radius, color) => new ClosedModel[Unit](
        Unit,
        new PolygonRing(
          renderStack.MaterialXYZRGB, 40, Seq.fill(40)(color), Seq.fill(40)(color),
          radius - 2, radius, VertexXY(0, 0), 0, 0
        ).noCaching.getModel,
        modelview)
      case rectangle: DrawRectangle => new ClosedModel[Unit](
        Unit,
        RectangleModelBuilder(rectangle.bounds).getModel,
        modelview)
    }
  }

  def obtainModelview(pos: PositionDescriptor)(implicit renderStack: RenderStack): Matrix4x4 = {
    if (pos.cachedModelviewMatrix.isEmpty) {
      val xPos = pos.x
      val yPos = pos.y
      val orientation = pos.orientation
      val modelviewMatrix =
        if (renderStack.modelviewTranspose) new RotationZTranslationXYTransposedMatrix4x4(orientation, xPos, yPos)
        else new RotationZTranslationXYMatrix4x4(orientation, xPos, yPos)
      pos.cachedModelviewMatrix = modelviewMatrix
    }
    pos.cachedModelviewMatrix.get
  }

}
