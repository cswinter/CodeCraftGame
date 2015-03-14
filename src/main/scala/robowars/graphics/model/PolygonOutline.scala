package robowars.graphics.model

import robowars.graphics.materials.Material

import scala.reflect.ClassTag


case class PolygonOutline[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  n: Int,
  colorInside: TColor,
  colorOutside: TColor,
  innerRadius: Float,
  outerRadius: Float,
  position: VertexXY = NullVectorXY,
  zPos: Float = 0,
  orientation: Float = 0
) extends PrimitiveModelBuilder[PolygonOutline[TColor, TParams], TColor, TParams] {
  val shape = this


  protected def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val innerVertices = Geometry.polygonVertices(n, orientation, innerRadius, position)
    val outerVertices = Geometry.polygonVertices(n, orientation, outerRadius, position)
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
      colors(2 * 3 * i + 1) = colorOutside
      colors(2 * 3 * i + 2) = colorOutside
      colors(2 * 3 * i + 4) = colorOutside

      colors(2 * 3 * i + 0) = colorInside
      colors(2 * 3 * i + 3) = colorInside
      colors(2 * 3 * i + 5) = colorInside
    }

    vertexPos zip colors
  }
}
