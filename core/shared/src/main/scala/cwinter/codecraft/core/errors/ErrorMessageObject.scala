package cwinter.codecraft.core.errors

import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.util.maths.{ColorRGBA, Vector2}

private[core] class ErrorMessageObject(
  val message: String,
  val errorLevel: ErrorLevel,
  var position: Vector2,
  val lifetime: Int = 120
) {
  private[this] var age = 0

  def update(): Unit = {
    val color = ColorRGBA(errorLevel.color, 1 - (age.toFloat * age / (lifetime * lifetime)))
    Debug.drawText(message, position.x.toFloat, position.y.toFloat, color)
    position += Vector2(0, 0.66f)
    age += 1
  }

  def hasFaded: Boolean = age >= lifetime
}
