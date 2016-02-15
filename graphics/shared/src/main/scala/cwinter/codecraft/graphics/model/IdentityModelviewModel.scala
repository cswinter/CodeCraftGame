package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.{Matrix4x4, IdentityMatrix4x4}


private[graphics] class IdentityModelviewModel[T](val model: Model[T]) extends Model[T] {
  def update(params: T): Unit =
    model.update(params)

  def setVertexCount(n: Int): Unit = model.setVertexCount(n)

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    model.draw(IdentityMatrix4x4, material)

  def hasMaterial(material: GenericMaterial): Boolean =
    model.hasMaterial(material)

  def vertexCount = model.vertexCount
}
