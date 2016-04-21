package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.VertexXYZ
import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[codecraft] trait Model[T] {
  def update(params: T): Unit
  def setVertexCount(n: Int): Unit

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit

  def hasMaterial(material: GenericMaterial): Boolean

  def vertexCount: Int

  def scalable(transpose: Boolean = false): ScalableModel[T] = new ScalableModel(this, transpose)
  def identityModelview: IdentityModelviewModel[T] = new IdentityModelviewModel[T](this)
  def translated(amount: VertexXYZ, transpose: Boolean): TranslatedModel[T] =
    new TranslatedModel[T](this, amount, transpose)
  def withDynamicVertexCount: DynamicVertexCountModel[T] = new DynamicVertexCountModel[T](this)
  def wireParameters[S](projection: S => T): ProjectedParamsModel[S, T] =
    new ProjectedParamsModel(this, projection)

  def prettyPrintTree(depth: Int): String

  def prettyTreeView: String = prettyPrintTree(0)
  protected def prettyPrintNode(depth: Int, contents: String): String = {
    if (depth == 0) contents
    else "   " * (depth - 1) + "+--" + contents
  }
  protected def prettyPrintWrapper(depth: Int, contents: String, child: Model[_]): String = {
    val rootNode = prettyPrintNode(depth, contents)
    val childTree = child.prettyPrintTree(depth + 1)
    s"$rootNode\n$childTree"
  }
}

