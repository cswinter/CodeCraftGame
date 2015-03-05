package robowars.graphics.primitives

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix2x2
import robowars.graphics.model._

import scala.reflect.ClassTag


class Primitive2D[TColor <: Vertex : ClassTag] private
(val positions: Array[VertexXY], val colors: Array[TColor], material: Material[VertexXYZ, TColor])
  extends ModelBuilder[VertexXYZ, TColor](material) {
  assert(positions.forall(_ != null))

  def this(positions: Array[VertexXY], material: Material[VertexXYZ, TColor]) =
    this(positions, new Array[TColor](positions.length), material)

  private[this] var _zPos: Float = 0

  def zPos(z: Float): Primitive2D[TColor] = {
    _zPos = z
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

  def scale(s: Float): Primitive2D[TColor] =
    mapPos{case VertexXY(x, y) => VertexXY(s * x, s * y)}

  def scaleX(x: Float): Primitive2D[TColor] =
    scale(x, 1)

  def scaleY(y: Float): Primitive2D[TColor] =
    scale(1, y)


  def vertexData =
    positions.map {case VertexXY(x, y) => VertexXYZ(x, y, _zPos) } zip colors

  @inline
  protected def mapPos(f: VertexXY => VertexXY): Primitive2D[TColor] = {
    positions.transform(f)
    this
  }

  @inline
  protected def mapCol(f: TColor => TColor): Primitive2D[TColor] = {
    colors.transform(f)
    this
  }
}
