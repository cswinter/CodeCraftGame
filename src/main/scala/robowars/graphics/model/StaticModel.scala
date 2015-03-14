package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix4x4


class StaticModel[TPosition <: Vertex, TColor <: Vertex, TParams](
  val vbo: VBO,
  val material: Material[TPosition, TColor, TParams]
) extends Model[TParams] {

  def update(params: TParams): Unit = { material.params = params }

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    if (material == this.material)
      material.draw(vbo, modelview)

  def hasMaterial(material: GenericMaterial): Boolean =
    this.material == material
}
