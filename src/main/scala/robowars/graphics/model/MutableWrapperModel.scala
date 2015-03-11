package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix4x4


class MutableWrapperModel(private var model: DrawableModel) extends DrawableModel {
  def replaceModel(model: DrawableModel): Unit = {
    this.model = model
  }

  override def init(): DrawableModel = ???

  override def +(model: Model): Model =
    throw new UnsupportedOperationException("You cannot sum a MutableModel. Compose with * instead.")

  override def project(material: Material[_, _]): DrawableModel = model.project(material)

  override def hasMaterial(material: Material[_, _]): Boolean = model.hasMaterial(material)

  override def draw(): Unit = model.draw()

  override def setModelview(modelview: Matrix4x4): Unit = model.setModelview(modelview)
}
