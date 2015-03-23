package robowars.graphics.model

import robowars.graphics.materials.Material

import scala.reflect.ClassTag



trait PrimitiveModelBuilder[TShape, TColor <: Vertex, TParams] <: ModelBuilder[TShape, TParams] {
  val material: Material[VertexXYZ, TColor, TParams]
  val shape: TShape
  private[this] var _cacheable = true
  def signature = shape

  protected def buildModel: Model[TParams] = {
    val vbo = material.createVBO(computeVertexData())
    new StaticModel(vbo, material)
  }

  override def isCacheable = _cacheable
  def noCaching: this.type = {
    _cacheable = false
    this
  }

  protected def computeVertexData(): Seq[(VertexXYZ, TColor)]
}




