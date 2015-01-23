package robowars.graphics.model

import robowars.graphics.matrices._

class ConcreteModel(val vbo: VBO, var modelview: Matrix4x4, val material: Material[_, _]) extends DrawableModel {
  def draw(): Unit = {
    material.draw(vbo, modelview)
  }

  def setModelview(modelview: Matrix4x4): Unit = {
    this.modelview = modelview
  }

  def init() = this

  def +(model: Model): Model = throw new UnsupportedOperationException(
    "Cannot sum initialised models.")

  def project(material: Material[_, _]): DrawableModel = material match {
    case this.material => this
    case _ => EmptyModel
  }

  def hasMaterial(material: Material[_, _]): Boolean =
    material == this.material
}


object ConcreteModel {
  def apply[TPos <: Vertex, TCol <: Vertex](material: Material[TPos, TCol], vertices: Seq[(TPos, TCol)]) =
    new ConcreteModel(material.createVBO(vertices), IdentityMatrix4x4, material)
}
