package robowars.graphics.primitives

import com.sun.javaws.exceptions.InvalidArgumentException
import robowars.graphics.materials.Material
import robowars.graphics.model._

import scala.reflect.ClassTag


class QuadStrip[TColor <: Vertex : ClassTag](
  width: Float,
  points: Seq[VertexXY]
)(material: Material[VertexXYZ, TColor])
  extends Primitive2D[TColor](QuadStrip.computePositions(Seq.fill(points.size)(width / 2), points), material) {
}

object QuadStrip {
  def computePositions(widths: Seq[Float], midpoints: Seq[VertexXY]) = {
    val n = midpoints.length

    if (n < 2)
      throw new IllegalArgumentException("there must be at least two midpoints")
    if (n != widths.size)
      throw new IllegalArgumentException("each midpoint must have an associated width")

    val data = new Array[VertexXY]((n - 1) * 3 * 3 - 3)
    val direction = new Array[VertexXY](n - 1)
    val normals = new Array[VertexXY](n - 1)

    for (i <- 0 until n - 1)
    {
      direction(i) = (midpoints(i + 1) - midpoints(i)).normalized
      normals(i) = VertexXY(direction(i).y, -direction(i).x)
    }

    for (i <- 0 until n -1)
    {
      val p1 = midpoints(i) + normals(i) * widths(i)
      val p2 = midpoints(i + 1) + normals(i) * widths(i + 1)
      val p3 = midpoints(i + 1) - normals(i) * widths(i + 1)
      val p4 = midpoints(i) - normals(i) * widths(i)

      if (i < n - 2) {
        val q1 = midpoints(i + 1) + normals(i + 1) * widths(i + 1)
        val q4 = midpoints(i + 1) - normals(i + 1) * widths(i + 1)
        val q1mp2 = q1 - p2
        val dot = q1mp2 dot direction(i)

        if (dot > 0) {
          data(3 * 3 * i + 6) = p2
          data(3 * 3 * i + 7) = midpoints(i + 1)
          data(3 * 3 * i + 8) = q1
        } else {
          data(3 * 3 * i + 6) = p3
          data(3 * 3 * i + 7) = midpoints(i + 1)
          data(3 * 3 * i + 8) = q4
        }
      } else {
        println("wat? (Line.scala)")
      }

      data(3 * 3 * i + 0) = p1
      data(3 * 3 * i + 1) = p2
      data(3 * 3 * i + 2) = p3
      data(3 * 3 * i + 3) = p1
      data(3 * 3 * i + 4) = p3
      data(3 * 3 * i + 5) = p4
    }

    data
  }
}
