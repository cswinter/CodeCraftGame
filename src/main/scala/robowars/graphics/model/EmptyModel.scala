package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix4x4

object EmptyModel extends DrawableModel with ComposableModel {
   def draw() = { }
   def setModelview(modelview: Matrix4x4): Unit = { }
   def init(): DrawableModel = this
   def +(model: ComposableModel) = model
   def project(material: Material[_, _, _]) = this
   def hasMaterial(material: Material[_, _, _]) = false
 }
