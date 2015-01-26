package robowars.graphics.model

import robowars.graphics.materials.Material

abstract class ModelBuilder[TPosition <: Vertex, TColor <: Vertex]
(val material: Material[TPosition, TColor])
  extends Model {
  self =>

  def init(): ConcreteModel =
    ConcreteModel[TPosition, TColor](material, vertexData)


  def +(other: Model): Model = {
    other match {
      case mb: ModelBuilder[TPosition, TColor] if mb.material == material =>
        new ModelBuilder[TPosition, TColor](material) {
          def vertexData = self.vertexData ++ mb.vertexData
        }
      case mb: ModelBuilder[_, _] =>
        new CompositeModelBuilder(
          Map(material.asInstanceOf[GenericMaterial] -> this.asInstanceOf[GenericModelBuilder],
            mb.material.asInstanceOf[GenericMaterial] -> mb.asInstanceOf[GenericModelBuilder]))
      case _ => other + this
    }
  }

  def project(material: Material[_, _]): Model =
    if (this.material == material) this
    else EmptyModel

  def hasMaterial(material: Material[_, _]): Boolean = material == this.material

  def vertexData: Seq[(TPosition, TColor)]
}
