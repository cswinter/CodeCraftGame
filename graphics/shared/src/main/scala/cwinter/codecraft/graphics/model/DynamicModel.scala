package cwinter.codecraft.graphics.model


private[graphics] class DynamicModel[T](
  val modelFactory: T => Model[Unit]
) extends DecoratorModel[T, Unit] {
  private[this] var _model: Model[Unit] = null
  def model: Model[Unit] = _model

  override protected def displayString: String = "Dynamic"
  override def hasMaterial(material: GenericMaterial): Boolean =
    model == null || model.hasMaterial(material)
  override def update(params: T): Unit =_model = modelFactory(params)
}

