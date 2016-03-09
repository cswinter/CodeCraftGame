package cwinter.codecraft.graphics.model


private[graphics] case class ProjectedParamsModel[T, U](
  model: Model[U],
  projection: T => U
) extends DecoratorModel[T, U] {
  override def update(params: T) = model.update(projection(params))
  override protected def displayString: String = "ProjectParameters"
}

