package cwinter.graphics.primitives

import cwinter.graphics.materials.Material
import cwinter.graphics.matrices.Matrix2x2
import cwinter.graphics.model._

import scala.reflect.ClassTag

// TODO: port to new architecture
/*
class RichCircleSegment[TColor <: Vertex : ClassTag](
  val sides: Int,
  val width: Float,
  material: Material[VertexXYZ, TColor, _])
  extends Primitive2D[TColor](RichCircleSegment.computeVertices(sides, width), material) {

  def colorMidpoint(color: TColor): this.type = {
    for (i <- 0 until sides) {
      colors(3 * i) = color
    }
    this
  }

  def colorOutside(color: TColor): this.type = {
    for (i <- 0 until sides) {
      colors(3 * i + 1) = color
      colors(3 * i + 2) = color
    }
    this
  }
}


object RichCircleSegment {
  /**
   * Creates a circle segment, with one flat side and one curved side.
   * The length of the flat side is 2.
   * @param sides The number of triangles used.
   * @param width The diameter of the figure perpendicular to the flat side.
   * @return
   */
  def computeVertices(sides: Int, width: Float): Array[VertexXY] = {
    val positions = new Array[VertexXY](3 * sides)

    val radius = 0.5f * (1 / width + width)

    val midpoint = VertexXY(radius - width, 0)
    val pos0 = VertexXY(0, 1) - midpoint
    val pos1 = VertexXY(0, -1) - midpoint
    val centerPos = VertexXY(0, 0) - midpoint

    val totalAngle =
      if (width <= 1) math.acos((pos0 dot pos1) / (pos0.size * pos1.size)).toFloat
      else 2 * math.Pi.toFloat - math.acos((pos0 dot pos1) / (pos0.size * pos1.size)).toFloat

    val angle = totalAngle / sides
    val rotation = Matrix2x2.rotation(angle)

    var last = pos0
    for (i <- Range(0, sides)) {
      val next = rotation * last

      positions(3 * i + 0) = centerPos + midpoint
      positions(3 * i + 1) = last + midpoint
      positions(3 * i + 2) = next + midpoint

      last = next
    }

    positions
  }
}
*/