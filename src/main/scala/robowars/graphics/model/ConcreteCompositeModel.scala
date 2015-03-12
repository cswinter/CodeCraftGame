package robowars.graphics.model

import robowars.graphics.materials.Material

import language.existentials

import robowars.graphics.matrices._

class ConcreteCompositeModel(val models: Map[GenericMaterial, ConcreteModel])
  extends DrawableModel {

  def draw(): Unit =
    throw new UnsupportedOperationException("Cannot draw composite model (project onto material first)")

  def setModelview(modelview: Matrix4x4): Unit = {
    for (model <- models.values)
      model.setModelview(modelview)
  }

  def project(material: Material[_, _]): DrawableModel = models.get(material.asInstanceOf[GenericMaterial]) match {
    case Some(model) => model
    case None => EmptyModel
  }

  def hasMaterial(material: Material[_, _]): Boolean =
    models.contains(material.asInstanceOf[GenericMaterial])
}
