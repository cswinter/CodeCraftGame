package cwinter.codecraft.util.maths

import scala.scalajs.js.annotation.JSExport

/* NOTE: using @JSExportAll or exporting the case class fields individually seems to trigger a Scala.js bug or something
  where calling the Vector2 constructor in the optimized Javascript will throw an exception. Hence this workaround */
/**
 * Immutable 2D Vector.
 *
 * There are two ways to create vectors: `Vector2(x, y)` will create a vector with components x and y and
 * `Vector2(a)` will create a unit vector rotated by an angle of `a` radians.
 * (In the JavaScript version, create vectors using `Game.vector2(x, y)`.)
 * Addition, subtraction, multiplication and division work as expected:
 * {{{
 * >> 2 * Vector2(0.5, 0) + Vector2(0, 10) / 10 - Vector2(10, 0)
 * res0: Vector2 = Vector2(-9, 1)
 * }}}
 *
 * @param _x The x coordinate.
 * @param _y The y coordinate.
 */
@JSExport
final case class Vector2(_x: Double, _y: Double) {
  import Vector2._
  assert(isValid)

  /** Returns the x-coordinate */
  @JSExport @inline def x = _x
  /** Returns the y-coordinate */
  @JSExport @inline def y = _y
  /** Returns the dot product of the vector with `rhs`. */
  @JSExport def dot(rhs: Vector2): Double = x * rhs.x + y * rhs.y
  /** Returns the sum of the vector and `rhs`. */
  @JSExport def +(rhs: Vector2): Vector2 = Vector2(x + rhs.x, y + rhs.y)
  /** Returns the difference of the vector and `rhs`. */
  @JSExport def -(rhs: Vector2): Vector2 = Vector2(x - rhs.x, y - rhs.y)
  /** Returns the scalar product of the vector with `rhs` */
  @JSExport def *(rhs: Double): Vector2 = Vector2(x * rhs, y * rhs)
  /** Returns the scalar product of the vector with `rhs` */
  @JSExport def *(rhs: Float): Vector2 = Vector2(x * rhs, y * rhs)
  /** Returns the scalar product of the vector with `rhs` */
  @JSExport def *(rhs: Int): Vector2 = Vector2(x * rhs, y * rhs)
  /** Returns the vector divided by the scalar `rhs` */
  @JSExport def /(rhs: Double): Vector2 = this * (1.0 / rhs)
  /** Returns the square of the magnitude of the vector */
  @JSExport def lengthSquared = x * x + y * y
  /** Returns the magnitude of the vector */
  @JSExport def length: Double = math.sqrt(x * x + y * y)
  /** Returns a new vector with the same direction and whose magnitude is equal to 1 */
  @JSExport def normalized: Vector2 = this / length
  /** Returns the vector rotated by an angle of `angle` radians. */
  @JSExport def rotated(angle: Double): Vector2 = {
    val sina = math.sin(angle)
    val cosa = math.cos(angle)
    Vector2(cosa * x - sina * y, sina * x + cosa * y)
  }

  /** Returns the sum of this vector and `rhs`. */
  @JSExport def plus(rhs: Vector2): Vector2 = this + rhs
  /** Returns the sum of this vector and `rhs`. */
  @JSExport def minus(rhs: Vector2): Vector2 = this - rhs
  /** Returns the scalar product of the vector with `rhs` */
  @JSExport def times(rhs: Double): Vector2 = this * rhs

  /**
   * Returns the angle in radians of the vector and the x-axis.
   * This value is always between 0 and 2 * Pi.
   */
  @JSExport def orientation = {
    assert(x != 0 || y != 0, s"x=$x, y=$y")
    val atan = math.atan2(y, x)
    if (atan > 0) atan else atan + 2 * math.Pi
  }

  /**
   * Returns true if none of the components of the vectors are +-infinity or NaN, otherwise false.
   */
  @JSExport def isValid: Boolean = {
    !x.isNaN && !y.isNaN && !x.isInfinity && !y.isInfinity
  }

  private[codecraft] def ~(rhs: Vector2): Boolean =
    math.abs(x - rhs.x) < epsilon && math.abs(y - rhs.y) < epsilon
  private[codecraft] def !~(rhs: Vector2): Boolean =
    math.abs(x - rhs.x) >= epsilon || math.abs(y - rhs.y) >= epsilon

  /**
   * Returns the additive inverse of the vector.
   */
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

  private[codecraft] def apply(string: String): Vector2 = {
    string match {
      case Vector2Regex(xStr, yStr) => Vector2(xStr.toDouble, yStr.toDouble)
    }
  }

  private[codecraft] def apply(vertexXY: VertexXY): Vector2 =
    Vector2(vertexXY.x, vertexXY.y)

  private[codecraft] final val Vector2Regex = """Vector2\((.*?),(.*?)\)""".r
  private[codecraft] def unapply(string: String): Option[Vector2] = string match {
    case Vector2Regex(xStr, yStr) => Some(Vector2(xStr.toDouble, yStr.toDouble))
    case _ => None
  }

  val Null = Vector2(0, 0)
}
