package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] class ClosedModel[T](objectState: T, model: Model[T], modelview: Matrix4x4) {
  def draw(material: GenericMaterial): Unit = {
    if (model.hasMaterial(material)) {
      model.update(objectState)
      model.draw(modelview, material)
    }
  }
}
