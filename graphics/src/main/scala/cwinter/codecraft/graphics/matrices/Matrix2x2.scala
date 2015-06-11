package cwinter.codecraft.graphics.matrices

import cwinter.codecraft.util.maths.VertexXY


final case class Matrix2x2(m11: Float, m12: Float, m21: Float, m22: Float) {
  def *(other: Matrix2x2): Matrix2x2 =
    Matrix2x2(
      m11 * other.m11 + m12 * other.m21, m11 * other.m12 + m12 * other.m22,
      m12 * other.m11 + m22 * other.m21, m12 * other.m21 + m22 * other.m22
    )

  def *(other: VertexXY): VertexXY =
    VertexXY(
      m11 * other.x + m12 * other.y,
      m21 * other.x + m22 * other.y
    )
}


object Matrix2x2 {
  def rotation(angle: Float): Matrix2x2 = {
    val cos = math.cos(angle).toFloat
    val sin = math.sin(angle).toFloat
    Matrix2x2(
      cos, -sin,
      sin, cos
    )
  }

  def scale(s: Float): Matrix2x2 =
    Matrix2x2(
      s, 0,
      0, s
    )

  def scale(x: Float, y: Float): Matrix2x2 =
    Matrix2x2(
      x, 0,
      0, y
    )

  def scaleX(x: Float): Matrix2x2 =
    Matrix2x2(
      x, 0,
      0, 1
    )

  def scaleY(y: Float): Matrix2x2 =
    Matrix2x2(
      1, 0,
      0, y
    )
}
