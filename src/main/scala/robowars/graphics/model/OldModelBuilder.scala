package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices.{IdentityMatrix4x4, Matrix4x4}

abstract class OldModelBuilder[TPosition <: Vertex, TColor <: Vertex]
(val material: Material[TPosition, TColor, _])
  extends ComposableModel {
  self =>

  def init(): ConcreteModel =
    ConcreteModel[TPosition, TColor](material, vertexData)

  /*def initParameterized[TParams](params: Parameterized[TParams]): ConcreteModel with Parameterized[TParams] =
    ConcreteModel[TPosition, TColor, TParams](material, vertexData, params)
  */

  def +(other: ComposableModel): ComposableModel = {
    other match {
      case mb: OldModelBuilder[TPosition, TColor] if mb.material == material =>
        new OldModelBuilder[TPosition, TColor](material) {
          def vertexData = self.vertexData ++ mb.vertexData
        }
      case mb: OldModelBuilder[_, _] =>
        new CompositeModelBuilder(
          Map(material.asInstanceOf[GenericMaterial] -> this.asInstanceOf[GenericModelBuilder],
            mb.material.asInstanceOf[GenericMaterial] -> mb.asInstanceOf[GenericModelBuilder]))
      case _ => other + this
    }
  }

  def project(material: Material[_, _, _]): OldModel =
    if (this.material == material) this
    else OldEmptyModel

  def hasMaterial(material: Material[_, _, _]): Boolean = material == this.material

  def vertexData: Seq[(TPosition, TColor)]
}
