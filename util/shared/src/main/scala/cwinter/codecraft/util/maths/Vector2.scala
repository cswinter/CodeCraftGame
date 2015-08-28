package cwinter.codecraft.util.maths

import scala.scalajs.js.annotation.JSExport

/* NOTE: using @JSExportAll or exporting the case class fields individually seems to trigger a Scala.js bug or something
  where calling the Vector2 constructor in the optimized Javascript will throw an exception. Hence this workaround */
@JSExport
case class Vector2(_x: Double, _y: Double) {
  import Vector2._
  assert(isValid)

  @JSExport @inline final def x = _x
  @JSExport @inline final def y = _y
  @JSExport def dot(rhs: Vector2): Double = x * rhs.x + y * rhs.y
  @JSExport def +(rhs: Vector2): Vector2 = Vector2(x + rhs.x, y + rhs.y)
  @JSExport def -(rhs: Vector2): Vector2 = Vector2(x - rhs.x, y - rhs.y)
  @JSExport def *(rhs: Double): Vector2 = Vector2(x * rhs, y * rhs)
  @JSExport def *(rhs: Float): Vector2 = Vector2(x * rhs, y * rhs)
  @JSExport def *(rhs: Int): Vector2 = Vector2(x * rhs, y * rhs)
  @JSExport def /(rhs: Double): Vector2 = this * (1.0 / rhs)
  @JSExport def lengthSquared = x * x + y * y
  @JSExport def length: Double = math.sqrt(x * x + y * y)
  @JSExport def normalized: Vector2 = this / length
  @JSExport def rotated(angle: Double): Vector2 = {
    val sina = math.sin(angle)
    val cosa = math.cos(angle)
    Vector2(cosa * x - sina * y, sina * x + cosa * y)
  }

  @JSExport def plus(rhs: Vector2): Vector2 = this + rhs
  @JSExport def minus(rhs: Vector2): Vector2 = this - rhs
  @JSExport def times(rhs: Double): Vector2 = this * rhs

  @JSExport def orientation = {
    assert(x != 0 || y != 0, s"x=$x, y=$y")
    val atan = math.atan2(y, x)
    if (atan > 0) atan else atan + 2 * math.Pi
  }

  @JSExport def isValid: Boolean = {
    !x.isNaN && !y.isNaN && !x.isInfinity && !y.isInfinity
  }

  def ~(rhs: Vector2): Boolean =
    math.abs(x - rhs.x) < epsilon && math.abs(y - rhs.y) < epsilon
  def !~(rhs: Vector2): Boolean =
    math.abs(x - rhs.x) >= epsilon || math.abs(y - rhs.y) >= epsilon

  @JSExport def unary_- = Vector2(-x, -y)
}

object Vector2 {
  final val epsilon: Double = 0.00000000001

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

  def apply(string: String): Vector2 = {
    string match {
      case Vector2Regex(xStr, yStr) => Vector2(xStr.toDouble, yStr.toDouble)
    }
  }

  def apply(vertexXY: VertexXY): Vector2 =
    Vector2(vertexXY.x, vertexXY.y)

  final val Vector2Regex = """Vector2\((.*?),(.*?)\)""".r
  def unapply(string: String): Option[Vector2] = string match {
    case Vector2Regex(xStr, yStr) => Some(Vector2(xStr.toDouble, yStr.toDouble))
    case _ => None
  }

  val Null = Vector2(0, 0)
}
