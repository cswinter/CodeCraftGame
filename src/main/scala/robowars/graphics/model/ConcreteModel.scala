package robowars.graphics.model

import robowars.graphics.matrices._

class ConcreteModel[TPosition <: Vertex, TColor <: Vertex]
(material: Material[TPosition, TColor], vertices: Seq[(TPosition, TColor)])
  extends DrawableModel {

  val vbo = material.createVBO(vertices)
  var modelview: Matrix4x4 = IdentityMatrix4x4

  def draw(): Unit = {
    material.draw(vbo, modelview)
  }

  def setModelview(modelview: Matrix4x4): Unit = {
    this.modelview = modelview
  }

  def init() = this

  def +(model: Model): Model = throw new UnsupportedOperationException(
    "Cannot sum initialised models.")

  def project(material: Material[_, _]): Model = material match {
    case this.material => this
    case _ => EmptyModel
  }

  def hasMaterial(material: Material[_, _]): Boolean =
    material == this.material
}
