package cwinter.graphics.model

import cwinter.codinggame.util.maths.{Vertex, VertexXY, VertexXYZ}
import cwinter.graphics.materials.Material

import scala.reflect.ClassTag


case class SquarePrimitive[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  midpointX: Float,
  midpointY: Float,
  width: Float,
  color: TColor,
  zPos: Float
) extends PrimitiveModelBuilder[SquarePrimitive[TColor, TParams], TColor, TParams] {
  val shape = this

  protected override def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    val p1 = VertexXYZ(midpointX + width, midpointY + width, zPos)
    val p2 = VertexXYZ(midpointX - width, midpointY + width, zPos)
    val p3 = VertexXYZ(midpointX - width, midpointY - width, zPos)
    val p4 = VertexXYZ(midpointX + width, midpointY - width, zPos)

    val vertexPos =
      Seq(
        p1, p2, p3,
        p1, p3, p4
      )

    val colors = vertexPos.map(_ => color)

    vertexPos zip colors
  }
}

