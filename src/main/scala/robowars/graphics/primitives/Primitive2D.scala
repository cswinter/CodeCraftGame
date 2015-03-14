package robowars.graphics.primitives

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix2x2
import robowars.graphics.model._

import scala.reflect.ClassTag


class Primitive2D[TColor <: Vertex : ClassTag] private
(val positions: Array[VertexXY], val colors: Array[TColor], material: Material[VertexXYZ, TColor, _])
  extends OldModelBuilder[VertexXYZ, TColor](material) {
  assert(positions.forall(_ != null))

  def this(positions: Array[VertexXY], material: Material[VertexXYZ, TColor, _]) =
    this(positions, new Array[TColor](positions.length), material)

  private[this] var _zPos: Float = 0

  def zPos(z: Float): this.type = {
    _zPos = z
    this
  }

  def color(color: TColor): this.type =
    mapCol(_ => color)

  def color(color: Seq[TColor]): this.type = {
    val iter = color.iterator
    mapCol(_ => iter.next())
  }

  def colorTriangles(color: Seq[TColor]): this.type =
    this.color(color ++ color ++ color)

  def translate(x: Float, y: Float): this.type =
    mapPos(pos => VertexXY(pos.x + x, pos.y + y))

  def translate(amount: VertexXY): this.type =
    translate(amount.x, amount.y)

  def rotate(a: Float): this.type = {
    val rotationMat = Matrix2x2.rotation(a)
    mapPos(rotationMat * _)
  }

  def scale(x: Float, y: Float): this.type = {
    val scaleMat = Matrix2x2.scale(x, y)
    mapPos(scaleMat * _)
  }

  def scale(s: Float): this.type =
    mapPos{case VertexXY(x, y) => VertexXY(s * x, s * y)}

  def scaleX(x: Float): this.type =
    scale(x, 1)

  def scaleY(y: Float): this.type =
    scale(1, y)


  def vertexData =
    positions.map {case VertexXY(x, y) => VertexXYZ(x, y, _zPos) } zip colors

  @inline
  protected def mapPos(f: VertexXY => VertexXY): this.type = {
    positions.transform(f)
    this
  }

  @inline
  protected def mapCol(f: TColor => TColor): this.type = {
    colors.transform(f)
    this
  }
}
