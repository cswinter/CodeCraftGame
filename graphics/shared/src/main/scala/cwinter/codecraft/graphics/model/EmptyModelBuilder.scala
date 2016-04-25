package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.engine.GraphicsContext

private[codecraft] object EmptyModelBuilder extends ModelBuilder[Unit, Unit] {

  protected def buildModel(context: GraphicsContext) = EmptyModel

  override def isCacheable = false

  def signature = ()
}
