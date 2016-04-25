package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.engine.GraphicsContext

private[graphics] case class ProjectedParamsModelBuilder[
    TStatic, TDynamic, UDynamic](
    model: ModelBuilder[TStatic, UDynamic],
    projection: TDynamic => UDynamic
) extends ModelBuilder[TStatic, TDynamic] {
  def signature: TStatic = model.signature

  protected def buildModel(context: GraphicsContext): Model[TDynamic] =
    model.getModel(context).wireParameters(projection)

  override def isCacheable: Boolean = false
}
