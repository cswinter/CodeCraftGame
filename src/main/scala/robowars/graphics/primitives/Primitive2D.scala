package robowars.graphics.primitives

import robowars.graphics.matrices.Matrix2x2
import robowars.graphics.model._


class Primitive2D[TColor <: Vertex]
(val vertices: Array[(VertexXY, TColor)], material: Material[VertexXY, TColor])
  extends ModelBuilder[VertexXY, TColor](material, vertices) {

  private[this] var _zPos: Float = 1

  def zPos(z: Float): Primitive2D[TColor] = {
    _zPos = 1
    this
  }

  def color(color: TColor): Primitive2D[TColor] =
    mapCol(_ => color)

  def color(color: Seq[TColor]): Primitive2D[TColor] = {
    val iter = color.iterator
    mapCol(_ => iter.next())
  }

  def colorTriangles(color: Seq[TColor]): Primitive2D[TColor] =
    this.color(color ++ color ++ color)

  def translate(x: Float, y: Float): Primitive2D[TColor] =
    mapPos(pos => VertexXY(pos.x + x, pos.y + y))

  def rotate(a: Float): Primitive2D[TColor] = {
    val rotationMat = Matrix2x2.rotation(a)
    mapPos(rotationMat * _)
  }

  def scale(x: Float, y: Float): Primitive2D[TColor] = {
    val scaleMat = Matrix2x2.scale(x, y)
    mapPos(scaleMat * _)
  }

  def scaleX(x: Float): Primitive2D[TColor] =
    scale(x, 1)

  def scaleY(y: Float): Primitive2D[TColor] =
    scale(1, y)


  @inline
  private def map(f: ((VertexXY, TColor)) => (VertexXY, TColor)): Primitive2D[TColor] = {
    vertices.transform(f)
    this
  }

  @inline
  private def mapPos(f: VertexXY => VertexXY): Primitive2D[TColor] = {
    vertices.transform { case (pos, col) => (f(pos), col)}
    this
  }

  @inline
  private def mapCol(f: TColor => TColor): Primitive2D[TColor] = {
    vertices.transform { case (pos, col) => (pos, f(col))}
    this
  }
}
