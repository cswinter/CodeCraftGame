package cwinter.codecraft.graphics.model


private[graphics] trait CompositeModelBuilder[TStatic, TDynamic]
extends ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic

  protected def build: (Seq[ModelBuilder[_, Unit]], Seq[ModelBuilder[_, TDynamic]])

  def buildModel: Model[TDynamic] = {
    val (staticComponents, dynamicComponents) = build
    decorate(CompositeModel(
      staticComponents.map(_.getModel),
      dynamicComponents.map(_.getModel)
    ))
  }

  protected def decorate(model: Model[TDynamic]): Model[TDynamic] = model
}

