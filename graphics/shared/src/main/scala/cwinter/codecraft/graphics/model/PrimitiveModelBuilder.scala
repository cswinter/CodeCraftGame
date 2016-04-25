package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.engine.GraphicsContext
import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.PrecomputedHashcode
import cwinter.codecraft.util.maths.{Vertex, VertexXYZ}

private[graphics] trait PrimitiveModelBuilder[
    TShape, TColor <: Vertex, TParams]
    extends ModelBuilder[TShape, TParams] with PrecomputedHashcode {
  self: Product =>

  val material: Material[VertexXYZ, TColor, TParams]
  val shape: TShape
  private[this] var _cacheable = true
  def signature = shape

  protected def buildModel(context: GraphicsContext): Model[TParams] = {
    val vbo = material.createVBO(computeVertexData(), !_cacheable)
    if (!_cacheable) context.createdTempVBO(vbo)
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

