package robowars.graphics.primitives

import robowars.graphics.materials.Material
import robowars.graphics.model._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag


class QuadStrip[TColor <: Vertex : ClassTag](
  width: Float,
  points: Seq[VertexXY]
)(material: Material[VertexXYZ, TColor, _])
  extends Primitive2D[TColor](QuadStrip.computePositions(width, points), material) {

  val n = points.length


  def colorMidpoints(color: Seq[TColor]): this.type = {
    assert(color.length == n)
    // data layout: start, start, end; start, end, end; connector, connector, connector
    for (i <- 0 until n - 1) {
      colors(9 * i + 0) = color(i)
      colors(9 * i + 1) = color(i)
      colors(9 * i + 2) = color(i + 1)

      colors(9 * i + 3) = color(i)
      colors(9 * i + 4) = color(i + 1)
      colors(9 * i + 5) = color(i + 1)

      if (i < n - 2) {
        colors(9 * i + 6) = color(i + 1)
        colors(9 * i + 7) = color(i + 1)
        colors(9 * i + 8) = color(i + 1)
      }
    }
    this
  }
}


object QuadStrip {
  def computePositions(width: Float, midpoint: Seq[VertexXY]) = {
    // diagram: https://www.dropbox.com/sc/owb97vdjnl7bxq0/AAAg0qFJNR5lyxoB4RG7OLJ6a

    val n = midpoint.length

    if (n < 2)
      throw new IllegalArgumentException("there must be at least two midpoints")
    //if (n != width.size)
    //  throw new IllegalArgumentException("each midpoint must have an associated width")


    /** compute directions, normals and left/right points and connector points **/
    val direction = new Array[VertexXY](n - 1)
    val normal = new Array[VertexXY](n - 1)
    val leftStart = new Array[VertexXY](n - 1)
    val rightStart = new Array[VertexXY](n - 1)
    val leftEnd = new Array[VertexXY](n - 1)
    val rightEnd = new Array[VertexXY](n - 1)
    val connector = new Array[VertexXY](n - 1)
    for (i <- 0 until n - 1)
    {
      direction(i) = (midpoint(i + 1) - midpoint(i)).normalized
      normal(i) = VertexXY(-direction(i).y, direction(i).x)

      leftStart(i) = midpoint(i) + 0.5f * width * normal(i)
      rightStart(i) = midpoint(i) - 0.5f * width * normal(i)
      leftEnd(i) = midpoint(i + 1) + 0.5f * width * normal(i)
      rightEnd(i) = midpoint(i + 1) - 0.5f * width * normal(i)

      if (i > 0) {
        // diagram for connector: https://www.dropbox.com/sc/teodl7o29d5z9kg/AADRaqm_Vd4CSfUzI4HFdCmSa

        // determine on which side the connector point is required
        val leftConnector = ((midpoint(i) - leftStart(i)) dot direction(i - 1)) > 0

        val end = if (leftConnector) leftEnd(i - 1) else rightEnd(i - 1)
        val start = if (leftConnector) leftStart(i) else rightStart(i)
        val dd = direction(i) + direction(i - 1)
        val es = end - start

        // alpha * dd = ll
        val alpha = (Math.abs(es.x) + Math.abs(es.y)) / (Math.abs(dd.x) + Math.abs(dd.y))

        connector(i) = direction(i) * alpha + start

        /** reassign outside curve points to the connector as appropriate **/
        if (leftConnector) {
          leftStart(i) = connector(i)
          leftEnd(i - 1) = connector(i)
        } else {
          rightStart(i) = connector(i)
          rightEnd(i - 1) = connector(i)
        }
      }
    }

    /** set triangle data **/
    val data = new ArrayBuffer[VertexXY]((n - 1) * 3 * 3 - 3)
    for (i <- 0 until n -1)
    {
      // fill-in at point i
      if (i > 0) {
        if (leftStart(i) == leftEnd(i - 1)) {
          data += leftStart(i)
          data += rightEnd(i - 1)
          data += rightStart(i)
          // enabling check increases efficiency, but makes midpoint coloring awkward
        } else { // if (rightStart(i) == rightEnd(i - 1)) {
          data += rightStart(i)
          data += leftStart(i)
          data += leftEnd(i - 1)
        }
      }

      // quad from point i to point i + 1
      data += leftStart(i)
      data += rightStart(i)
      data += rightEnd(i)

      data += leftStart(i)
      data += rightEnd(i)
      data += leftEnd(i)

    }

    data.toArray
  }
}
