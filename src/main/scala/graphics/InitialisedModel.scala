package graphics

class InitialisedModel(material: Material, vertices: Array[Float]) extends Model {
  val vbo = material.createVBO(vertices)

  def draw(): Unit = {
    material.draw(vbo)
  }

  def init(): Model = this

  def +(model: Model): Model = throw new UnsupportedOperationException(
    "Cannot sum initialised models.")

  def project(material: Material): Model = material match {
    case this.material => this
    case _ => EmptyModel
  }

  def hasMaterial(material: Material): Boolean =
    material == this.material
}
