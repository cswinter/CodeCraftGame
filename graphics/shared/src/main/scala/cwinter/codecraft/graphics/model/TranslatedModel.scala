package cwinter.codecraft.graphics.model

import cwinter.codecraft.util.maths.VertexXYZ
import cwinter.codecraft.util.maths.matrices.{TranslationMatrix4x4, DilationXYMatrix4x4, Matrix4x4}


private[graphics] class TranslatedModel[T](
  val model: Model[T],
  translation: VertexXYZ,
  transpose: Boolean = false
) extends ParameterPreservingDecoratorModel[T] {

  import translation._
  private val translationMatrix =
    if (transpose) new TranslationMatrix4x4(x, y, z).transposed
    else new TranslationMatrix4x4(x, y, z)

  override def draw(modelview: Matrix4x4, material: GenericMaterial): Unit = {
    val translatedModelview =
      if (transpose) modelview * translationMatrix
      else translationMatrix * modelview
    model.draw(translatedModelview, material)
  }

  override protected def displayString: String = "Translated"
}

