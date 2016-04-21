package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4

import scala.annotation.tailrec


private[codecraft] case class CompositeModel[T](
  staticModels: Seq[Model[Unit]],
  dynamicModels: Seq[Model[T]]
) extends Model[T] {
  val models =
    if (staticModels.isEmpty) dynamicModels
    else if (dynamicModels.isEmpty) staticModels
    else staticModels ++ dynamicModels


  def update(params: T): Unit = for (model <- dynamicModels) model.update(params)

  def setVertexCount(n: Int): Unit = {
    require(n >= 0)
    _setVertexCount(n, models)
  }

  @tailrec
  private def _setVertexCount(remaining: Int, models: Seq[Model[_]]): Unit = models match {
    case Seq(model, _*) =>
      val count = model.vertexCount
      val allocated = math.min(count, remaining)
      model.setVertexCount(allocated)
      _setVertexCount(remaining - allocated, models.tail)
    case Seq() =>
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

  def prettyPrintTree(depth: Int): String = {
    val root = prettyPrintNode(depth, s"Composite(${models.length})")
    val children =
      for (model <- models) yield model.prettyPrintTree(depth + 1)

    root + "\n" + children.mkString("\n")
  }
}

