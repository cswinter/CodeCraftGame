package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.{IdentityMatrix4x4, Matrix4x4}


private[graphics] class IdentityModelviewModel[T](
  val model: Model[T]
) extends ParameterPreservingDecoratorModel[T] {
  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    model.draw(IdentityMatrix4x4, material)
  override protected def displayString: String = "IdentityModelview"
}

