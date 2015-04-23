package cwinter.codinggame.maths

case class Vector2(x: Double, y: Double) {
  def dot(rhs: Vector2): Double = x * rhs.x + y * rhs.y
  def +(rhs: Vector2): Vector2 = Vector2(x + rhs.x, y + rhs.y)
  def -(rhs: Vector2): Vector2 = Vector2(x - rhs.x, y - rhs.y)
  def *(rhs: Double): Vector2 = Vector2(x * rhs, y * rhs)
  def *(rhs: Float): Vector2 = Vector2(x * rhs, y * rhs)
  def *(rhs: Int): Vector2 = Vector2(x * rhs, y * rhs)
  def /(rhs: Double): Vector2 = this * (1.0 / rhs)
  def magnitudeSquared = x * x + y * y
  def size: Double = math.sqrt(x * x + y * y)
  def normalized: Vector2 = this / size

  def orientation = {
    assert(x != 0 || y != 0, s"x=$x, y=$y")
    math.atan2(y, x)
  }

  final val epsilon: Double = 0.00000000001
  def ~(rhs: Vector2): Boolean =
    math.abs(x - rhs.x) < epsilon && math.abs(y - rhs.y) < epsilon

  def unary_- = Vector2(-x, -y)
}

object Vector2 {
  implicit class ScalarD(val d: Double) extends AnyVal {
    def *(rhs: Vector2): Vector2 = rhs * d
  }

  implicit class ScalarF(val f: Float) extends AnyVal {
    def *(rhs: Vector2): Vector2 = rhs * f
  }

  implicit class ScalarI(val i: Int) extends AnyVal {
    def *(rhs: Vector2): Vector2 = rhs * i
  }

  def apply(angle: Double): Vector2 =
    Vector2(math.cos(angle).toFloat, math.sin(angle).toFloat)

  val NullVector = Vector2(0, 0)
}
