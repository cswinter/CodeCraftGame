package cwinter.codecraft.graphics.primitives

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.PrimitiveModelBuilder
import cwinter.codecraft.util.maths.{Vertex, VertexXY, VertexXYZ}

import scala.reflect.ClassTag


private[graphics] case class LinePrimitive[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  p1: VertexXY,
  p2: VertexXY,
  width: Float,
  colorInside: TColor,
  colorOutside: TColor,
  zPos: Float
) extends PrimitiveModelBuilder[LinePrimitive[TColor, TParams], TColor, TParams] {
  val shape = this

  protected override def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val offset = width * (p1 - p2).perpendicular.normalized

    val upperLeft = p1 + offset
    val upperRight = p1 - offset
    val downLeft = p2 + offset
    val downRight = p2 - offset

    val vertexPos =
      Seq(
        upperLeft, downLeft, p1,
        p1, downLeft, p2,
        p1, p2, upperRight,
        upperRight, p2, downRight
      )

    val colors = Seq(
      colorOutside, colorOutside, colorInside,
      colorInside, colorOutside, colorInside,
      colorInside, colorInside, colorOutside,
      colorOutside, colorInside, colorOutside
    )

    vertexPos.map(_.zPos(zPos)) zip colors
  }
}
