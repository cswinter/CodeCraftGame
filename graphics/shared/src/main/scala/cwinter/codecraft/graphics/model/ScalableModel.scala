package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.{DilationXYMatrix4x4, Matrix4x4}


private[graphics] class ScalableModel[T](
  val model: Model[T],
  transpose: Boolean = false
) extends DecoratorModel[(T, Float), T] {

  private[this] var scale = 1.0f

  def update(params: (T, Float)): Unit = {
    model.update(params._1)
    scale = params._2
  }

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = {
    val scaledModelview =
      if (transpose) modelview * new DilationXYMatrix4x4(scale)
      else new DilationXYMatrix4x4(scale) * modelview
    model.draw(scaledModelview, material)
  }

  override protected def displayString: String = "Scalable"
}

