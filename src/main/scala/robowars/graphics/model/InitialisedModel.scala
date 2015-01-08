package robowars.graphics.model

import robowars.graphics.matrices._

class InitialisedModel(material: Material, vertices: Array[Float]) extends Model {
  val vbo = material.createVBO(vertices)
  var modelview: Matrix4x4 = IdentityMatrix4x4

  def draw(): Unit = {
    material.draw(vbo, modelview)
  }

  def init() = this

  def +(model: Model): Model = throw new UnsupportedOperationException(
    "Cannot sum initialised models.")

  def project(material: Material): Model = material match {
    case this.material => this
    case _ => EmptyModel
  }

  def hasMaterial(material: Material): Boolean =
    material == this.material
}
