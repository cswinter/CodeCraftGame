package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix4x4


class DrawableProductModel(model1: DrawableModel, model2: DrawableModel) extends DrawableModel {
  override def draw(): Unit = {
    model1.draw()
    model2.draw()
  }

  override def setModelview(modelview: Matrix4x4): Unit = {
    model1.setModelview(modelview)
    model2.setModelview(modelview)
  }

  override def project(material: Material[_, _, _]): DrawableModel = {
    (model1.project(material), model2.project(material)) match {
      case (OldEmptyModel, OldEmptyModel) => OldEmptyModel
      case (OldEmptyModel, p2) => p2
      case (p1, OldEmptyModel) => p1
      case (p1, p2) => new DrawableProductModel(p1, p2)
    }
  }

  override def hasMaterial(material: Material[_, _, _]): Boolean =
    model1.hasMaterial(material) || model2.hasMaterial(material)
}
