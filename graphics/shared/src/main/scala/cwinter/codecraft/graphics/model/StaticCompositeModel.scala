package cwinter.codecraft.graphics.model


private[graphics] class StaticCompositeModel(val models: Seq[Model[Unit]]) extends CompositeModel[Unit] {
  def update(params: Unit): Unit = { }
}
