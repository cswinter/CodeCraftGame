package cwinter.codinggame.maths

case class Vector2(x: Float, y: Float) {
  def dot(rhs: Vector2): Float = x * rhs.x + y * rhs.y
  def +(rhs: Vector2): Vector2 = Vector2(x + rhs.x, y + rhs.y)
  def -(rhs: Vector2): Vector2 = Vector2(x - rhs.x, y - rhs.y)
  def *(rhs: Float): Vector2 = Vector2(x * rhs, y * rhs)
  def *(rhs: Int): Vector2 = Vector2(x * rhs, y * rhs)

  def unary_- = Vector2(-x, -y)
}

object Vector2 {
  implicit class ScalarF(val f: Float) extends AnyVal {
    def *(rhs: Vector2): Vector2 = rhs * f
  }

  implicit class ScalarI(val i: Int) extends AnyVal {
    def *(rhs: Vector2): Vector2 = rhs * i
  }

  def apply(angle: Double): Vector2 =
    Vector2(math.cos(angle).toFloat, math.sin(angle).toFloat)
}
