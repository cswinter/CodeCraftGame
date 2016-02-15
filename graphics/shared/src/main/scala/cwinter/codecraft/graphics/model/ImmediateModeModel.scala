package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] class ImmediateModeModel extends Model[Seq[Model[Unit]]] {
  private[this] var models = new StaticCompositeModel(Seq())

  override def update(params: Seq[Model[Unit]]): Unit =
    models = new StaticCompositeModel(params)

  override def setVertexCount(n: Int): Unit =
    models.setVertexCount(n)

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    models.draw(modelview, material)

  override def vertexCount: Int = models.vertexCount

  override def hasMaterial(material: GenericMaterial): Boolean = models.hasMaterial(material)

  def prettyPrintTree(depth: Int): String =
    prettyPrintNode(depth, "ImmediateModeModel")
}
