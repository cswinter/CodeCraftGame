package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.engine.RenderStack
import cwinter.codecraft.graphics.primitives.PolygonRing
import cwinter.codecraft.util.maths.matrices._
import cwinter.codecraft.graphics.model.{EmptyModel, Model, ClosedModel}
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.VertexXY


private[graphics] object TheWorldObjectModelFactory {
  def generateModel[T](modelDescriptor: ModelDescriptor[T], timestep: Int)
      (implicit renderStack: RenderStack): ClosedModel[T] = {

    val modelview = obtainModelview(modelDescriptor.position)
    val model = getModel(modelDescriptor.objectDescriptor, timestep)
    val parameters = modelDescriptor.objectParameters

    new ClosedModel[T](
      parameters,
      model,
      modelview
    )
  }

  def getModel[T](descriptor: WorldObjectDescriptor[T], timestep: Int)(implicit rs: RenderStack): Model[T] = {
    descriptor.cachedModel match {
      case Some(model) => model
      case None =>
        val model = createModel(descriptor, timestep)
        // FIXME: special case required because homing missile models are not cached. need to rework caching to fix this properly.
        if (!descriptor.isInstanceOf[HomingMissileDescriptor]) descriptor.cachedModel = model
        model
    }
  }

  def createModel[T](descriptor: WorldObjectDescriptor[T], timestep: Int)(implicit rs: RenderStack): Model[T] = {
    descriptor match {
      case mineral: MineralDescriptor => new MineralModelBuilder(mineral).getModel
      case drone: DroneDescriptor => new DroneModelBuilder(drone, timestep).getModel
      case h: HarvestingBeamsDescriptor => HarvestingBeamModelBuilder(h).getModel
      case c: ConstructionBeamDescriptor => ConstructionBeamsModelBuilder(c).getModel
      case lightFlash: LightFlashDescriptor => new LightFlashModelBuilder().getModel
      case HomingMissileDescriptor(positions, maxPos, player) =>
        HomingMissileModelFactory.build(positions, maxPos, player)
      case energyGlobe: EnergyGlobeDescriptor => new EnergyGlobeModelBuilder(energyGlobe).getModel
      case TestingObject(t) => new TestModelBuilder(t).getModel
      case circle: DrawCircle => CircleModelBuilder(circle.radius, circle.identifier)
      case rectangle: DrawRectangle => RectangleModelBuilder(rectangle.bounds).getModel
      case DrawCircleOutline(radius, color) =>
        new PolygonRing(
          rs.MaterialXYZRGB, 40, Seq.fill(40)(color), Seq.fill(40)(color),
          radius - 2, radius, VertexXY(0, 0), 0, 0
        ).noCaching.getModel
    }
  }.asInstanceOf[Model[T]]

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
