package cwinter.codinggame.graphics.model

import cwinter.codinggame.graphics.materials.Material
import cwinter.codinggame.graphics.matrices.Matrix4x4
import cwinter.codinggame.util.maths.Vertex


class StaticModel[TPosition <: Vertex, TColor <: Vertex, TParams](
  val vbo: VBO,
  val material: Material[TPosition, TColor, TParams]
) extends Model[TParams] {
  private[this] var activeVertexCount = vbo.size

  def update(params: TParams): Unit = { material.params = params }

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    if (material == this.material)
      material.draw(vbo.copy(size = activeVertexCount), modelview)

  def hasMaterial(material: GenericMaterial): Boolean =
    this.material == material

  def setVertexCount(n: Int): Unit = {
    assert(n % 3 == 0)
    assert(n >= 0)
    assert(n <= vertexCount)
    activeVertexCount = n
  }
  def vertexCount = vbo.size
}
