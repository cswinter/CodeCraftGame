package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] class HideableModel[T](
  val model: Model[T]
) extends DecoratorModel[(IsHidden, T), T] {
  private[this] var show = true

  override def update(params: (IsHidden, T)): Unit = {
    val (isHidden, baseParams) = params
    show = !isHidden.value
    if (show) model.update(baseParams)
  }

  override def setVertexCount(n: Int): Unit =
    if (show) model.setVertexCount(n)

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    if (show) model.draw(modelview, material)

  override protected def displayString: String = "Hideable"
}

private[graphics] case class IsHidden(value: Boolean) extends AnyVal



