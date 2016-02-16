package cwinter.codecraft.graphics.model


private[graphics] class StaticCompositeModel(
  models: Seq[Model[Unit]]
) extends CompositeModel[Unit](models, Seq.empty)

