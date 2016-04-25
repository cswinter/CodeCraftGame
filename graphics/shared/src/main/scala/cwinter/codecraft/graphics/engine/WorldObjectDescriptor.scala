package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.model.{ClosedModel, Model}
import cwinter.codecraft.util.PrecomputedHashcode
import cwinter.codecraft.util.maths._
import cwinter.codecraft.util.maths.matrices.{Matrix4x4, RotationZTranslationXYMatrix4x4, RotationZTranslationXYTransposedMatrix4x4}


private[codecraft] trait WorldObjectDescriptor[T] extends PrecomputedHashcode {
  self: Product =>

  private[this] var _rs: RenderStack = null
  implicit protected def rs: RenderStack = _rs

  private var cachedModel = Option.empty[Model[T]]


  def intersects(xPos: Float, yPos: Float, rectangle: Rectangle): Boolean = true

  @inline
  final protected def intersects(x: Float, y: Float, width: Float, rectangle: Rectangle): Boolean = {
    x + width > rectangle.xMin &&
    x - width < rectangle.xMax &&
    y + width > rectangle.yMin &&
    y - width < rectangle.yMax
  }

  def model(timestep: Int, context: GraphicsContext): Model[T] = {
    cachedModel match {
      case Some(model) => model
      case None =>
        _rs = context.materials
        val model = getModel(context)
        if (allowCaching) cachedModel = Some(model)
        model
    }
  }

  protected def getModel(context: GraphicsContext): Model[T]
  protected def allowCaching: Boolean = true
}

