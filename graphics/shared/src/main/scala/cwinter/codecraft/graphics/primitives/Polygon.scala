package cwinter.codecraft.graphics.primitives

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.PrimitiveModelBuilder
import cwinter.codecraft.util.maths._

import scala.reflect.ClassTag


private[codecraft] case class Polygon[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  n: Int,
  colorMidpoint: Seq[TColor],
  colorOutside: Seq[TColor],
  radius: Float,
  position: VertexXY,
  zPos: Float,
  orientation: Float,
  colorEdges: Boolean
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
    if (colorEdges) {
      for (i <- 0 until n) {
        colors(3 * i + 1) = colorOutside(i)
        colors(3 * i + 2) = colorOutside(i)
        colors(3 * i) = colorMidpoint(i)
      }
    } else { // color vertices
      for (i <- 0 until n) {
        colors(3 * i + 1) = colorOutside(i)
        colors(3 * i + 2) = colorOutside((i + 1) % n)
        colors(3 * i) = colorMidpoint(i)
      }
    }

    vertexPos zip colors
  }
}

private[codecraft] object Polygon {
  def apply[TColor <: Vertex : ClassTag, TParams](
    material: Material[VertexXYZ, TColor, TParams],
    n: Int,
    colorMidpoint: TColor,
    colorOutside: TColor,
    radius: Float = 1,
    position: VertexXY = NullVectorXY,
    zPos: Float = 0,
    orientation: Float = 0,
    colorEdges: Boolean = true
  ): Polygon[TColor, TParams] =
    Polygon(material, n, Seq.fill(n)(colorMidpoint), Seq.fill(n)(colorOutside),
      radius, position, zPos, orientation, colorEdges = colorEdges)
}
