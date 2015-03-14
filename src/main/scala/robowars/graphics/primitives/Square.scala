package robowars.graphics.primitives

import robowars.graphics.materials.Material
import robowars.graphics.model._

import scala.reflect.ClassTag


class Square[TColor <: Vertex : ClassTag]
(material: Material[VertexXYZ, TColor, _])
  extends Primitive2D[TColor](Square.computePositions(1), material) {
}


object Square {
  private def computePositions(sideLength: Float): Array[VertexXY] = {
    val x = sideLength / 2
    Array[VertexXY](
      new VertexXY(x, x),
      new VertexXY(x, -x),
      new VertexXY(-x, -x),

      new VertexXY(x, x),
      new VertexXY(-x, -x),
      new VertexXY(-x, x)
    )
  }
}