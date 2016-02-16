package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] trait DecoratorModel[T, U] extends Model[T] {
  protected def model: Model[U]
  protected def displayString: String

  override def setVertexCount(n: Int): Unit = model.setVertexCount(n)
  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = model.draw(modelview, material)
  override def prettyPrintTree(depth: Int): String = prettyPrintWrapper(depth, displayString, model)
  override def vertexCount: Int = model.vertexCount
  override def hasMaterial(material: GenericMaterial): Boolean = model.hasMaterial(material)
}

