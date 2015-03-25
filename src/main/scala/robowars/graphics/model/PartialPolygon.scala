package robowars.graphics.model

import robowars.graphics.materials.Material

import scala.reflect.ClassTag


case class PartialPolygon[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  n: Int,
  colorMidpoint: Seq[TColor],
  colorOutside: Seq[TColor],
  radius: Float,
  position: VertexXY,
  zPos: Float,
  orientation: Float,
  fraction: Float
) extends PrimitiveModelBuilder[PartialPolygon[TColor, TParams], TColor, TParams] {
  val shape = this

  protected override def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val orientation = this.orientation + math.Pi.toFloat * (1 - fraction)

    val vertices =
      for (i <- 0 until n + 1)
        yield radius * VertexXY(fraction * i * 2 * math.Pi.toFloat / n + orientation) + position

    val vertexPos = new Array[VertexXYZ](3 * n)
    for (i <- 0 until n) {
      val v1 = vertices(i)
      val v2 = vertices(i + 1)

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


