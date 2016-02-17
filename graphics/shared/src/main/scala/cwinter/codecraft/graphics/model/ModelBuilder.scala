package cwinter.codecraft.graphics.model


private[graphics] trait ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic

  def getModel: Model[TDynamic] =
    if (isCacheable) TheModelCache.getOrElseUpdate(signature)(optimized.buildModel)
    else buildModel

  protected def buildModel: Model[TDynamic]

  def isCacheable: Boolean = true

  def optimized: ModelBuilder[TStatic, TDynamic] = this

  def wireParameters[SDynamic](projection: SDynamic => TDynamic) =
    new ProjectedParamsModelBuilder(this, projection)
}

