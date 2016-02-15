package cwinter.codecraft.graphics.primitives

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.PrimitiveModelBuilder
import cwinter.codecraft.util.maths.{Vertex, VertexXYZ}

import scala.reflect.ClassTag


private[graphics] case class SquarePrimitive[TColor <: Vertex : ClassTag, TParams](
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
