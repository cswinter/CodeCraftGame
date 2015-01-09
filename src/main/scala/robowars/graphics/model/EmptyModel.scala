package robowars.graphics.model

import robowars.graphics.matrices.Matrix4x4

object EmptyModel extends DrawableModel {
   def draw() = { }
   def setModelview(modelview: Matrix4x4): Unit = { }
   def init(): DrawableModel = this
   def +(model: Model) = model
   def project(material: Material) = this
   def hasMaterial(material: Material) = false
 }
