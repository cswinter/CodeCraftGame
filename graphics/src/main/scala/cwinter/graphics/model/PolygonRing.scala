package cwinter.graphics.model

import cwinter.graphics.materials.Material

import scala.reflect.ClassTag


case class PolygonRing[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  n: Int,
  colorInside: Seq[TColor],
  colorOutside: Seq[TColor],
  innerRadius: Float,
  outerRadius: Float,
  position: VertexXY,
  zPos: Float,
  orientation: Float
) extends PrimitiveModelBuilder[PolygonRing[TColor, TParams], TColor, TParams] {
  val shape = this

  protected def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val innerVertices = Geometry.polygonVertices2(n, orientation, innerRadius, position)
    val outerVertices = Geometry.polygonVertices2(n, orientation, outerRadius, position)
    val vertexPos = new Array[VertexXYZ](6 * n)

    for (i <- 0 until n) {
      val index1 = if (i == 0) n - 1 else i - 1
      val outer1 = outerVertices(index1)
      val outer2 = outerVertices(i)
      val inner1 = innerVertices(index1)
      val inner2 = innerVertices(i)

      vertexPos(6 * i + 0) = inner1.zPos(zPos)
      vertexPos(6 * i + 1) = outer1.zPos(zPos)
      vertexPos(6 * i + 2) = outer2.zPos(zPos)

      vertexPos(6 * i + 3) = inner1.zPos(zPos)
      vertexPos(6 * i + 4) = outer2.zPos(zPos)
      vertexPos(6 * i + 5) = inner2.zPos(zPos)
    }


    val colors = new Array[TColor](vertexPos.length)
    for (i <- 0 until n) {
      colors(2 * 3 * i + 1) = colorOutside(i)
      colors(2 * 3 * i + 2) = colorOutside(i)
      colors(2 * 3 * i + 4) = colorOutside(i)

      colors(2 * 3 * i + 0) = colorInside(i)
      colors(2 * 3 * i + 3) = colorInside(i)
      colors(2 * 3 * i + 5) = colorInside(i)
    }

    vertexPos zip colors
  }
}

object PolygonRing {
  def apply[TColor <: Vertex : ClassTag, TParams](
    material: Material[VertexXYZ, TColor, TParams],
    n: Int,
    colorInside: TColor,
    colorOutside: TColor,
    innerRadius: Float,
    outerRadius: Float,
    position: VertexXY = NullVectorXY,
    zPos: Float = 0,
    orientation: Float = 0
  ): PolygonRing[TColor, TParams] =
    this(material, n, Seq.fill(n)(colorInside), Seq.fill(n)(colorOutside), innerRadius, outerRadius, position, zPos, orientation)
}