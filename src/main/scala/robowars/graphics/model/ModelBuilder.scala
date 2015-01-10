package robowars.graphics.model

class ModelBuilder[TPosition <: Vertex, TColor <: Vertex]
(val material: Material[TPosition, TColor], val vertices: Seq[(TPosition, TColor)])
  extends Model {

  def init(): ConcreteModel[TPosition, TColor] =
    new ConcreteModel[TPosition, TColor](material, vertices)

  def +(other: Model): Model = {
    other match {
      case mb: ModelBuilder[TPosition, TColor] =>
        if (mb.material == material) new ModelBuilder[TPosition, TColor](material, vertices ++ mb.vertices)
        else ???
      case _ => ???
    }
  }

  def project(material: Material[_, _]): Model = material match {
    case this.material => this
    case _ => EmptyModel
  }

  def hasMaterial(material: Material[_, _]): Boolean = material == this.material
}
