package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4

import scala.annotation.tailrec


private[graphics] trait CompositeModel[T] <: Model[T] {
  def models: Seq[Model[_]]

  def update(params: T): Unit

  def setVertexCount(n: Int): Unit = {
    assert(n >= 0)

    @tailrec
    def _setVertexCount(remaining: Int, models: Seq[Model[_]]): Unit = {
      models.headOption match {
        case Some(model) =>
          val count = model.vertexCount
          val allocated = math.min(count, remaining)
          model.setVertexCount(allocated)
          _setVertexCount(remaining - allocated, models.tail)
        case None =>
      }
    }

    _setVertexCount(n, models)
  }

  // TODO: make more efficient (keep set of all materials?)
  def hasMaterial(material: GenericMaterial): Boolean =
    models.exists(_.hasMaterial(material))

  def draw(modelview: Matrix4x4, material: GenericMaterial): Unit =
    for {
      model <- models
      if model.hasMaterial(material)
    } model.draw(modelview, material)

  def vertexCount = models.map(_.vertexCount).sum
}
