package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix4x4


class MutableWrapperModel(private var model: DrawableModel) extends DrawableModel {
  def replaceModel(model: DrawableModel): Unit = {
    this.model = model
  }

  override def project(material: Material[_, _, _]): DrawableModel = model.project(material)

  override def hasMaterial(material: Material[_, _, _]): Boolean = model.hasMaterial(material)

  override def draw(): Unit = model.draw()

  override def setModelview(modelview: Matrix4x4): Unit = model.setModelview(modelview)
}
