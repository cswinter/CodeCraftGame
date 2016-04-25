package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.model.ClosedModel
import cwinter.codecraft.util.maths.Rectangle
import cwinter.codecraft.util.maths.matrices.{Matrix4x4, RotationZTranslationXYTransposedMatrix4x4, RotationZTranslationXYMatrix4x4}


private[codecraft] case class ModelDescriptor[T](
  position: PositionDescriptor,
  objectDescriptor: WorldObjectDescriptor[T],
  objectParameters: T
) {
  @inline
  final def intersects(rectangle: Rectangle): Boolean =
    objectDescriptor.intersects(position.x, position.y, rectangle)


  def closedModel(timestep: Int, context: GraphicsContext): ClosedModel[T] =
    new ClosedModel[T](objectParameters, objectDescriptor.model(timestep, context), modelview(context))

  private def modelview(context: GraphicsContext): Matrix4x4 = {
    if (position.cachedModelviewMatrix.isEmpty) {
      val xPos = position.x
      val yPos = position.y
      val orientation = position.orientation
      val modelviewMatrix =
        if (context.useTransposedModelview) new RotationZTranslationXYTransposedMatrix4x4(orientation, xPos, yPos)
        else new RotationZTranslationXYMatrix4x4(orientation, xPos, yPos)
      position.cachedModelviewMatrix = modelviewMatrix
    }
    position.cachedModelviewMatrix.get
  }
}

private[codecraft] object ModelDescriptor {
  def apply(position: PositionDescriptor, objectDescriptor: WorldObjectDescriptor[Unit]): ModelDescriptor[Unit] =
    ModelDescriptor(position, objectDescriptor, Unit)
}
