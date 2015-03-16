package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices._

class ConcreteModel(val vbo: VBO, var modelview: Matrix4x4, val material: Material[_, _, _]) extends DrawableModel {

  def draw(): Unit = {
    material.draw(vbo, modelview)
  }

  def setModelview(modelview: Matrix4x4): Unit = {
    this.modelview = modelview
  }

  def project(material: Material[_, _, _]): DrawableModel = material match {
    case this.material => this
    case _ => OldEmptyModel
  }

  def hasMaterial(material: Material[_, _, _]): Boolean =
    material == this.material
}


object ConcreteModel {
  def apply[TPos <: Vertex, TCol <: Vertex](material: Material[TPos, TCol, _], vertices: Seq[(TPos, TCol)]) =
    new ConcreteModel(material.createVBO(vertices), IdentityMatrix4x4, material)
/*
  def apply[TPos <: Vertex, TCol <: Vertex, TParams](
    material: Material[TPos, TCol, _],
    vertices: Seq[(TPos, TCol)],
    parameters: Parameterized[TParams]
  ) = {
    new ConcreteModel(material.createVBO(vertices), IdentityMatrix4x4, material) with ParameterizedConcreteModel[TParams] {
      val parameterSink: Parameterized[TParams] = parameters
    }
  }*/
}
