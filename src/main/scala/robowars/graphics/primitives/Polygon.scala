package robowars.graphics.primitives

import robowars.graphics.materials.Material
import robowars.graphics.matrices.Matrix2x2
import robowars.graphics.model._

import scala.reflect.ClassTag


class Polygon[TColor <: Vertex : ClassTag]
(val corners: Int, material: Material[VertexXYZ, TColor])
  extends Primitive2D[TColor](Polygon.computeVertices(corners), material) {

  def colorOutside(color: TColor): Polygon[TColor] = {
    for (i <- 0 until corners) {
      colors(3 * i + 1) = color
      colors(3 * i + 2) = color
    }
    this
  }

  def colorMidpoint(color: TColor): Polygon[TColor] = {
    for (i <- 0 until corners) {
      colors(3 * i) = color
    }
    this
  }
}

object Polygon {
  def computeVertices(corners: Int): Array[VertexXY] = {
    val angle = (2 * math.Pi / corners).toFloat
    val rotation = Matrix2x2.rotation(angle)
    val midpoint = new VertexXY(0, 0)

    var last = new VertexXY(1, 0)

    val positions = new Array[VertexXY](3 * corners)


    for (i <- 0 until corners) {
      val newpos = rotation * last

      positions(3 * i + 0) = midpoint
      positions(3 * i + 1) = last
      positions(3 * i + 2) = newpos

      last = newpos
    }

    positions
  }
}
