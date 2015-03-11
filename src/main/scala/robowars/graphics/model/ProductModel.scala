package robowars.graphics.model

import robowars.graphics.materials.Material


class ProductModel(val model1: Model, val model2: Model) extends Model {
  override def init(): DrawableModel =
    new DrawableProductModel(model1.init(), model2.init())

  override def +(model: Model): Model =
    throw new UnsupportedOperationException("Cannot sum product models")

  override def project(material: Material[_, _]): Model = ???

  override def hasMaterial(material: Material[_, _]): Boolean =
    model1.hasMaterial(material) || model2.hasMaterial(material)
}
