package cwinter.codecraft.graphics.model


private[graphics] class ImmediateModeModel
extends DecoratorModel[Seq[Model[Unit]], Unit] {
  private[this] var models = new StaticCompositeModel(Seq())
  def model: Model[Unit] = models

  override def update(params: Seq[Model[Unit]]): Unit =
    models = new StaticCompositeModel(params)

  override protected def displayString: String = "ImmediateMode"
}

