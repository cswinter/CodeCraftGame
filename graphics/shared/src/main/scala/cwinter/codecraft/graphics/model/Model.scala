package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4



private[graphics] trait Model[T] {
  def update(params: T): Unit
  def setVertexCount(n: Int): Unit

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit

  def hasMaterial(material: GenericMaterial): Boolean

  def vertexCount: Int

  def scalable(transpose: Boolean = false): ScalableModel[T] = new ScalableModel(this, transpose)
  def identityModelview: IdentityModelviewModel[T] = new IdentityModelviewModel[T](this)
}















