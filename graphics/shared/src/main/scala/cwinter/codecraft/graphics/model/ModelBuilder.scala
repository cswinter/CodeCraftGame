package cwinter.codecraft.graphics.model


private[graphics] trait ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic

  def getModel: Model[TDynamic] = {
    if (isCacheable) TheModelCache.getOrElseUpdate(signature)(buildModel)
    else buildModel
  }

  protected def buildModel: Model[TDynamic]

  def isCacheable: Boolean = true
}
