package cwinter.codecraft.graphics.models

import cwinter.codecraft.graphics.model._
import cwinter.codecraft.graphics.worldstate.WorldObjectDescriptor


private[graphics] case class TestModel(t: Int)
    extends ModelBuilder[TestModel, Unit] with WorldObjectDescriptor[Unit] {
  val sideLength = 50

  protected def buildModel: Model[Unit] = EmptyModel

  override def createModel(timestep: Int) = getModel
  override def isCacheable: Boolean = false
  override def signature = this
}

