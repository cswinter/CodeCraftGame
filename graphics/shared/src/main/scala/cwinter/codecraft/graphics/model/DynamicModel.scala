package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] class DynamicModel[T](val modelFactory: T => Model[Unit]) extends Model[T] {
  private[this] var model: Model[Unit] = null

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    model.draw(modelview, material)

  def setVertexCount(n: Int): Unit =
    model.setVertexCount(n)

  def hasMaterial(material: GenericMaterial): Boolean =
    model == null || model.hasMaterial(material)

  def update(params: T) = {
    model = modelFactory(params)
  }

  def vertexCount = model.vertexCount

  def prettyPrintTree(depth: Int): String =
    prettyPrintNode(depth, "Dynamic")
}

