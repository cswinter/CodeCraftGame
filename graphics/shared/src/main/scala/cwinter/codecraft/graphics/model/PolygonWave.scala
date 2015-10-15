package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.maths.{NullVectorXY, Vertex, VertexXY, VertexXYZ}

import scala.reflect.ClassTag


private[graphics] case class PolygonWave[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  n: Int,
  colorMidpoint: TColor,
  colorOutside: TColor,
  cycle: Int,
  radius: Float = 1,
  position: VertexXY = NullVectorXY,
  zPos: Float = 0,
  orientation: Float = 0
) extends PrimitiveModelBuilder[PolygonWave[TColor, TParams], TColor, TParams] {
  val shape = this

  assert(cycle >= 0)
  assert(cycle <= 100)

  protected override def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val vertices =
      for {
        i <- 0 until n
        offset = (1 - cycle / 100f) * 2 * math.sin(16 * math.Pi.toFloat * (i / n.toFloat)).toFloat
      } yield (radius + offset) * VertexXY(i * 2 * math.Pi.toFloat / n + orientation) + position

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
      colors(3 * i + 1) = colorOutside
      colors(3 * i + 2) = colorOutside
      colors(3 * i) = colorMidpoint
    }

    vertexPos zip colors
  }
}
