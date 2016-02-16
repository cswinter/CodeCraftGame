package cwinter.codecraft.graphics.model


private[graphics] case class ProjectedParamsModelBuilder[TStatic, TDynamic, UDynamic](
  model: ModelBuilder[TStatic, UDynamic],
  projection: TDynamic => UDynamic
) extends ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic = model.signature

  protected def buildModel: Model[TDynamic] = model.getModel.wireParameters(projection)

  override def isCacheable: Boolean = false
}

