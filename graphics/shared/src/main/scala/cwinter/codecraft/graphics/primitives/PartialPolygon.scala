package cwinter.codecraft.graphics.primitives

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.PrimitiveModelBuilder
import cwinter.codecraft.util.maths.{Vertex, VertexXY, VertexXYZ}

import scala.reflect.ClassTag


private[codecraft] case class PartialPolygon[TColor <: Vertex : ClassTag, TParams](
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
  require(colorMidpoint.size == n, s"Require n=$n midpoint colors. Actual: ${colorMidpoint.size}")
  require(colorOutside.size == 2 * n, s"Require 2*n=${2 * n} outside colors: Actual: ${colorOutside.size}")

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
      colors(3 * i + 1) = colorOutside(2 * i)
      colors(3 * i + 2) = colorOutside(2 * i + 1)
      colors(3 * i) = colorMidpoint(i)
    }

    vertexPos zip colors
  }
}
