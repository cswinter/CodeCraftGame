package robowars.graphics.model

import language.existentials


class CompositeModelBuilder(val models: Map[GenericMaterial, GenericModelBuilder])
  extends Model {
  self =>

  def init(): DrawableModel = {
    new ConcreteCompositeModel(models.map { case (material, model) => material -> model.init()})
  }

  def +(other: Model): Model = {
    other match {
      case mb: GenericModelBuilder =>
        if (models.contains(mb.material)) {
          val sum = (mb + models(mb.material)).asInstanceOf[GenericModelBuilder]
          new CompositeModelBuilder(models.updated(mb.material, sum))
        } else {
          new CompositeModelBuilder(models + (mb.material -> mb))
        }
      case cmb: CompositeModelBuilder =>
        val mergedModels: Set[(GenericMaterial, GenericModelBuilder)] =
          for (mat <- models.keySet | cmb.models.keySet) yield {
            if (hasMaterial(mat) && cmb.hasMaterial(mat)) {
              mat -> (models(mat) + cmb.models(mat)).asInstanceOf[GenericModelBuilder]
            } else if (hasMaterial(mat)) {
              mat -> cmb.models(mat)
            } else {
              mat -> models(mat)
            }
          }
        new CompositeModelBuilder(mergedModels.toMap)
    }
  }


  def project(material: Material[_, _]): Model = models.get(material.asInstanceOf[GenericMaterial]) match {
    case Some(modelBuilder) => modelBuilder
    case None => EmptyModel
  }

  def hasMaterial(material: Material[_, _]): Boolean =
    models.contains(material.asInstanceOf[GenericMaterial])
}
