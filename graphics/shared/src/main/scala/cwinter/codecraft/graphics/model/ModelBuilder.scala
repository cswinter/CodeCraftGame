package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.engine.GraphicsContext


private[codecraft] trait ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic

  def getModel(context: GraphicsContext): Model[TDynamic] =
    if (isCacheable) context.modelCache.getOrElseUpdate(signature)(optimized.buildModel(context))
    else buildModel(context)

  protected def buildModel(context: GraphicsContext): Model[TDynamic]

  def isCacheable: Boolean = true

  def optimized: ModelBuilder[TStatic, TDynamic] = this

  def wireParameters[SDynamic](projection: SDynamic => TDynamic) =
    ProjectedParamsModelBuilder(this, projection)
}

