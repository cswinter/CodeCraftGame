package cwinter.codinggame.core.errors

import cwinter.codinggame.graphics.engine.Debug
import cwinter.codinggame.util.maths.{ColorRGBA, Vector2}

private[core] class ErrorMessageObject(
  val message: String,
  val errorLevel: ErrorLevel,
  var position: Vector2,
  val lifetime: Int = 60
) {
  private[this] var age = 0

  def update(): Unit = {
    val color = ColorRGBA(errorLevel.color, 1 - (age.toFloat / lifetime))
    Debug.drawText(message, position.x.toFloat, position.y.toFloat, color)
    position += Vector2(0, 3)
    age += 1
  }

  def hasFaded: Boolean = age >= lifetime
}
