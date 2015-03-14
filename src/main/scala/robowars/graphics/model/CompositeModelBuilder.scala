package robowars.graphics.model

import robowars.graphics.materials.Material

import language.existentials

class CompositeModelBuilder(val models: Map[GenericMaterial, GenericModelBuilder])
  extends ComposableModel {
  self =>

  def init(): DrawableModel = {
    // need to do all of this because scala type system sucks
    val initModelIter: Iterable[(GenericMaterial, ConcreteModel)] =
      for (model <- models.values)
      yield (model.material, model.init())

    var initModelMap = Map.empty[GenericMaterial, ConcreteModel]

    for ((mat, mod) <- initModelIter) initModelMap += mat -> mod

    new ConcreteCompositeModel(initModelMap)
  }

  def +(other: ComposableModel): ComposableModel = {
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
      case EmptyModel => this
    }
  }

  def project(material: Material[_, _, _]): OldModel = models.get(material.asInstanceOf[GenericMaterial]) match {
    case Some(modelBuilder) => modelBuilder
    case None => EmptyModel
  }

  def hasMaterial(material: Material[_, _, _]): Boolean =
    models.contains(material.asInstanceOf[GenericMaterial])
}
