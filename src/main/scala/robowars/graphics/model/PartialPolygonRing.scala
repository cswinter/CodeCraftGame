package robowars.graphics.model

import robowars.graphics.materials.Material

import scala.reflect.ClassTag


case class PartialPolygonRing[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  n: Int,
  colorInside: Seq[TColor],
  colorOutside: Seq[TColor],
  innerRadius: Float,
  outerRadius: Float,
  position: VertexXY,
  zPos: Float,
  orientation: Float,
  fraction: Float
) extends PrimitiveModelBuilder[PartialPolygonRing[TColor, TParams], TColor, TParams] {
  val shape = this

  protected def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val orientation = this.orientation + math.Pi.toFloat * (1 - fraction)

    val innerVertices =
      for (i <- 0 until n + 1)
        yield innerRadius * VertexXY(fraction * i * 2 * math.Pi.toFloat / n + orientation) + position
    val outerVertices =
      for (i <- 0 until n + 1)
        yield outerRadius * VertexXY(fraction * i * 2 * math.Pi.toFloat / n + orientation) + position


    val vertexPos = new Array[VertexXYZ](6 * n)
    for (i <- 0 until n) {
      val outer1 = outerVertices(i)
      val outer2 = outerVertices(i + 1)
      val inner1 = innerVertices(i)
      val inner2 = innerVertices(i + 1)

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