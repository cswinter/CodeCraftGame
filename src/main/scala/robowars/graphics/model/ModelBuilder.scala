package robowars.graphics.model

abstract class ModelBuilder[TPosition <: Vertex, TColor <: Vertex]
(val material: Material[TPosition, TColor])
  extends Model {
  self =>

  def init(): ConcreteModel[TPosition, TColor] =
    new ConcreteModel[TPosition, TColor](material, vertexData)

  def +(other: Model): Model = {
    other match {
      case mb: ModelBuilder[TPosition, TColor] if mb.material == material =>
        new ModelBuilder[TPosition, TColor](material) {
          def vertexData = self.vertexData ++ mb.vertexData
        }
      case _ => ???
    }
  }

  def project(material: Material[_, _]): Model = material match {
    case this.material => this
    case _ => EmptyModel
  }

  def hasMaterial(material: Material[_, _]): Boolean = material == this.material

  def vertexData: Seq[(TPosition, TColor)]
}
