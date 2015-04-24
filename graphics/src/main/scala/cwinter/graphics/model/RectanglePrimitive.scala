package cwinter.graphics.model

import cwinter.graphics.materials.Material

import scala.reflect.ClassTag


case class RectanglePrimitive[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  xMin: Float,
  xMax: Float,
  yMin: Float,
  yMax: Float,
  width: Float,
  color: TColor,
  zPos: Float
) extends PrimitiveModelBuilder[RectanglePrimitive[TColor, TParams], TColor, TParams] {
  val shape = this

  protected override def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val vertexPos =
      Seq(
        // left side
        VertexXY(xMin, yMin),
        VertexXY(xMin + width, yMin + width),
        VertexXY(xMin + width, yMax - width),

        VertexXY(xMin, yMin),
        VertexXY(xMin + width, yMax - width),
        VertexXY(xMin, yMax),

        // top side
        VertexXY(xMin, yMax),
        VertexXY(xMin + width, yMax - width),
        VertexXY(xMax - width, yMax - width),

        VertexXY(xMin, yMax),
        VertexXY(xMax - width, yMax - width),
        VertexXY(xMax, yMax),

        // right side
        VertexXY(xMax, yMax),
        VertexXY(xMax - width, yMax - width),
        VertexXY(xMax - width, yMin + width),

        VertexXY(xMax, yMax),
        VertexXY(xMax - width, yMin + width),
        VertexXY(xMax, yMin),

        // bottom side
        VertexXY(xMin, yMin),
        VertexXY(xMax - width, yMin + width),
        VertexXY(xMin + width, yMin + width),

        VertexXY(xMin, yMin),
        VertexXY(xMax, yMin),
        VertexXY(xMax - width, yMin + width)
      )

    val colors = vertexPos.map(_ => color)

    vertexPos.map(_.zPos(zPos)) zip colors
  }
}
