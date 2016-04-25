package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.engine.GraphicsContext
import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.maths.{Vertex, VertexXYZ}


private[graphics] case class VertexCollectionModelBuilder[TColor <: Vertex](
  vertexData: Seq[Seq[(VertexXYZ, TColor)]],
  material: Material[VertexXYZ, TColor, Unit]
) extends ModelBuilder[Nothing, Unit] {

  def signature = throw new Exception("ConcreteVerticesModelBuilder has no signature.")

  protected def buildModel(context: GraphicsContext): Model[Unit] = {
    val vbo = material.createVBOSeq(vertexData, dynamic=false)
    new StaticModel(vbo, material)
  }

  override def isCacheable = false
}

