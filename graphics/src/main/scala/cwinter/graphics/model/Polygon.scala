package cwinter.graphics.model

import cwinter.codinggame.util.maths._
import cwinter.graphics.materials.Material

import scala.reflect.ClassTag


case class Polygon[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  n: Int,
  colorMidpoint: Seq[TColor],
  colorOutside: Seq[TColor],
  radius: Float,
  position: VertexXY,
  zPos: Float,
  orientation: Float
) extends PrimitiveModelBuilder[Polygon[TColor, TParams], TColor, TParams] {
  val shape = this

  protected override def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val vertices = Geometry.polygonVertices2(n, orientation, radius, position)
    val vertexPos = new Array[VertexXYZ](3 * n)
    for (i <- 0 until n) {
      val v1 = if (i == 0) vertices(n - 1) else vertices(i - 1)
      val v2 = vertices(i)

      vertexPos(3 * i + 0) = position.zPos(zPos)
      vertexPos(3 * i + 1) = v1.zPos(zPos)
      vertexPos(3 * i + 2) = v2.zPos(zPos)
    }


    val colors = new Array[TColor](vertexPos.length)
    for (i <- 0 until n) {
      colors(3 * i + 1) = colorOutside(i)
      colors(3 * i + 2) = colorOutside(i)
      colors(3 * i) = colorMidpoint(i)
    }

    vertexPos zip colors
  }
}

object Polygon {
  def apply[TColor <: Vertex : ClassTag, TParams](
    material: Material[VertexXYZ, TColor, TParams],
    n: Int,
    colorMidpoint: TColor,
    colorOutside: TColor,
    radius: Float = 1,
    position: VertexXY = NullVectorXY,
    zPos: Float = 0,
    orientation: Float = 0
  ): Polygon[TColor, TParams] =
    Polygon(material, n, Seq.fill(n)(colorMidpoint), Seq.fill(n)(colorOutside), radius, position, zPos, orientation)
}
