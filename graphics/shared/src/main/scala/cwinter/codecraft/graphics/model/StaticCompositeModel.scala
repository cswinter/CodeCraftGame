package cwinter.codecraft.graphics.model


private[codecraft] class StaticCompositeModel(
  models: Seq[Model[Unit]]
) extends CompositeModel[Unit](models, Seq.empty)

