package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices.{IdentityMatrix4x4, Matrix4x4}

trait DrawableModel extends OldModel {
  def draw(): Unit
  def setModelview(modelview: Matrix4x4)
  def project(material: Material[_, _, _]): DrawableModel
  def *(model: DrawableModel): DrawableModel =
    new DrawableProductModel(this, model)
}


class DrawableModelBridge(val model: Model[_]) extends DrawableModel {
  private[this] var material: GenericMaterial = null
  private[this] var modelview: Matrix4x4 = IdentityMatrix4x4

  override def draw(): Unit = model.draw(modelview, material)

  override def setModelview(modelview: Matrix4x4): Unit =
    this.modelview = modelview

  override def project(material: Material[_, _, _]): DrawableModel = {
    this.material = material.asInstanceOf[GenericMaterial]
    this
  }

  override def hasMaterial(material: Material[_, _, _]): Boolean =
    model.hasMaterial(material.asInstanceOf[GenericMaterial])
}