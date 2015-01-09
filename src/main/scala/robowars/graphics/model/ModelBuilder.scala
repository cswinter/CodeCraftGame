package robowars.graphics.model

class ModelBuilder(val material: Material, val vertices: Array[Float]) extends Model {

  def init(): InitialisedModel = new InitialisedModel(material, vertices)

  def +(other: Model): Model = {
    other match {
      case mb: ModelBuilder =>
        if (mb.material == material) new ModelBuilder(material, vertices ++ mb.vertices)
        else ???
      case _ => ???
    }
  }

  def project(material: Material): Model = material match {
    case this.material => this
    case _ => EmptyModel
  }


  def hasMaterial(material: Material): Boolean = material == this.material
}
