package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix4x4

trait DrawableModel extends Model {
  def draw(): Unit
  def setModelview(modelview: Matrix4x4)
  def project(material: Material[_, _]): DrawableModel
}
