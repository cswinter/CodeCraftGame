package cwinter.graphics.model

import javax.media.opengl.GL4

import cwinter.graphics.materials.Material

import scala.reflect.ClassTag



trait PrimitiveModelBuilder[TShape, TColor <: Vertex, TParams] <: ModelBuilder[TShape, TParams] {
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
}

object PrimitiveModelBuilder {
  private var toDispose = List.empty[VBO]

  def disposeAll()(implicit gl: GL4): Unit = {
    toDispose.foreach(_.dispose())
    toDispose = List.empty[VBO]
  }
}



