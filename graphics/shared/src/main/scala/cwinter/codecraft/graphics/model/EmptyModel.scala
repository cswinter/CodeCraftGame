package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] class EmptyModel[T] extends Model[T] {
  def update(params: T) = ()
  def setVertexCount(n: Int) = ()
  def draw(modelview: Matrix4x4, material: GenericMaterial) = ()
  def hasMaterial(material: GenericMaterial) = false
  def vertexCount = 0

  def prettyPrintTree(depth: Int): String =
    prettyPrintNode(depth, "Empty")
}

private[graphics] object EmptyModel extends EmptyModel[Unit]

