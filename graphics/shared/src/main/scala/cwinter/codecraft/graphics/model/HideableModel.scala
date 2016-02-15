package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] class HideableModel[T](val model: Model[T]) extends Model[(IsHidden, T)] {
  private[this] var show = true

  def update(params: (IsHidden, T)): Unit = {
    val (isHidden, baseParams) = params
    show = !isHidden.value
    if (show)
      model.update(baseParams)
  }

  def setVertexCount(n: Int): Unit =
    if (show) model.setVertexCount(n)

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    if (show)
      model.draw(modelview, material)

  def hasMaterial(material: GenericMaterial): Boolean =
    model.hasMaterial(material)

  def vertexCount = model.vertexCount

  def prettyPrintTree(depth: Int): String =
    prettyPrintWrapper(depth, "Hideable", model)
}

private[graphics] case class IsHidden(value: Boolean) extends AnyVal



