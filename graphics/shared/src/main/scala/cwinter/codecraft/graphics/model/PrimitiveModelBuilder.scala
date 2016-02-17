package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.maths.{Vertex, VertexXYZ}



private[graphics] trait PrimitiveModelBuilder[TShape, TColor <: Vertex, TParams]
extends ModelBuilder[TShape, TParams] {
  val material: Material[VertexXYZ, TColor, TParams]
  val shape: TShape
  private[this] var _cacheable = true
  def signature = shape

  protected def buildModel: Model[TParams] = {
    val vbo = material.createVBO(computeVertexData(), !_cacheable)
    if (!_cacheable) PrimitiveModelBuilder.toDispose ::= vbo
    new StaticModel(vbo, material)
  }

  override def isCacheable = _cacheable
  def noCaching: this.type = {
    _cacheable = false
    this
  }

  protected def computeVertexData(): Seq[(VertexXYZ, TColor)]

  def getVertexData: Seq[(VertexXYZ, TColor)] = computeVertexData()
}

private[graphics] object PrimitiveModelBuilder {
  private var toDispose = List.empty[VBO]

  def disposeAll(gl: Any): Unit = {
    toDispose.foreach(_.dispose(gl))
    toDispose = List.empty[VBO]
  }
}

