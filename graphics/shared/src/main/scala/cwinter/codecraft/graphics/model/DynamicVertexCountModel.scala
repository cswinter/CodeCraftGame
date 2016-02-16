package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.Float0To1
import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] case class DynamicVertexCountModel[T](
  model: Model[T]
) extends DecoratorModel[(Float0To1, T), T] {

  override def update(params: (Float0To1, T)): Unit = {
    model.update(params._2)
    val targetVertexCount = (params._1 * vertexCount / 3).toInt * 3
    model.setVertexCount(targetVertexCount)
  }

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = {
    model.draw(modelview, material)
    setVertexCount(Integer.MAX_VALUE)
  }

  override protected def displayString: String = "DynamicVertexCount"
}

