package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.engine.GraphicsContext

private[codecraft] trait SimpleModelBuilder[TStatic, TDynamic]
    extends ModelBuilder[TStatic, TDynamic] {

  protected def buildModel(context: GraphicsContext): Model[TDynamic] =
    model.getModel(context)

  protected def model: ModelBuilder[_, TDynamic]
}

