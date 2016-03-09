package cwinter.codecraft.graphics.model


private[graphics] trait ParameterPreservingDecoratorModel[T] extends DecoratorModel[T, T] {
  override def update(params: T): Unit = model.update(params)
}

