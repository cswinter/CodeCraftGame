package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.maths.{Vertex, VertexXY, VertexXYZ}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag


case class QuadStrip[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  midpoints: Seq[VertexXY],
  colors: Seq[TColor],
  width: Float,
  zPos: Float = 0
) extends PrimitiveModelBuilder[QuadStrip[TColor, TParams], TColor, TParams]{
  val shape: QuadStrip[TColor, TParams] = this
  val n = midpoints.length

  assert(colors.length == n, s"There are ${midpoints.length} midpoints and ${colors.length} colors")
  assert(n >= 2, "There must be at least two midpoints.")

  protected def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    // diagram: https://www.dropbox.com/sc/owb97vdjnl7bxq0/AAAg0qFJNR5lyxoB4RG7OLJ6a



    // compute directions, normals and left/right points and connector points
    val direction = new Array[VertexXY](n - 1)
    val normal = new Array[VertexXY](n - 1)
    val leftStart = new Array[VertexXY](n - 1)
    val rightStart = new Array[VertexXY](n - 1)
    val leftEnd = new Array[VertexXY](n - 1)
    val rightEnd = new Array[VertexXY](n - 1)
    val connector = new Array[VertexXY](n - 1)
    for (i <- 0 until n - 1)
    {
      direction(i) = (midpoints(i + 1) - midpoints(i)).normalized
      normal(i) = VertexXY(-direction(i).y, direction(i).x)

      leftStart(i) = midpoints(i) + 0.5f * width * normal(i)
      rightStart(i) = midpoints(i) - 0.5f * width * normal(i)
      leftEnd(i) = midpoints(i + 1) + 0.5f * width * normal(i)
      rightEnd(i) = midpoints(i + 1) - 0.5f * width * normal(i)

      if (i > 0) {
        // diagram for connector: https://www.dropbox.com/sc/teodl7o29d5z9kg/AADRaqm_Vd4CSfUzI4HFdCmSa

        // determine on which side the connector point is required
        val leftConnector = ((midpoints(i) - leftStart(i)) dot direction(i - 1)) > 0

        val end = if (leftConnector) leftEnd(i - 1) else rightEnd(i - 1)
        val start = if (leftConnector) leftStart(i) else rightStart(i)
        val dd = direction(i) + direction(i - 1)
        val es = end - start

        // alpha * dd = ll
        val alpha = (Math.abs(es.x) + Math.abs(es.y)) / (Math.abs(dd.x) + Math.abs(dd.y))

        connector(i) = direction(i) * alpha + start

        // reassign outside curve points to the connector as appropriate
        if (leftConnector) {
          leftStart(i) = connector(i)
          leftEnd(i - 1) = connector(i)
        } else {
          rightStart(i) = connector(i)
          rightEnd(i - 1) = connector(i)
        }
      }
    }

    // set triangle data
    val vertexPos = new ArrayBuffer[VertexXYZ]((n - 1) * 3 * 3 - 3)
    for (i <- 0 until n -1)
    {
      // fill-in at point i
      if (i > 0) {
        if (leftStart(i) == leftEnd(i - 1)) {
          vertexPos += leftStart(i).zPos(zPos)
          vertexPos += rightEnd(i - 1).zPos(zPos)
          vertexPos += rightStart(i).zPos(zPos)
          // enabling check increases efficiency, but makes midpoint coloring awkward
        } else { // if (rightStart(i) == rightEnd(i - 1)) {
          vertexPos += rightStart(i).zPos(zPos)
          vertexPos += leftStart(i).zPos(zPos)
          vertexPos += leftEnd(i - 1).zPos(zPos)
        }
      }

      // quad from point i to point i + 1
      vertexPos += leftStart(i).zPos(zPos)
      vertexPos += rightStart(i).zPos(zPos)
      vertexPos += rightEnd(i).zPos(zPos)

      vertexPos += leftStart(i).zPos(zPos)
      vertexPos += rightEnd(i).zPos(zPos)
      vertexPos += leftEnd(i).zPos(zPos)

    }


    val vertexCol = new Array[TColor](9 * (n - 1) - 3)
    for (i <- 0 until n - 1) {
      // data layout: start, start, end; start, end, end; connector, connector, connector
      vertexCol(9 * i + 0) = colors(i)
      vertexCol(9 * i + 1) = colors(i)
      vertexCol(9 * i + 2) = colors(i + 1)

      vertexCol(9 * i + 3) = colors(i)
      vertexCol(9 * i + 4) = colors(i + 1)
      vertexCol(9 * i + 5) = colors(i + 1)

      if (i < n - 2) {
        vertexCol(9 * i + 6) = colors(i + 1)
        vertexCol(9 * i + 7) = colors(i + 1)
        vertexCol(9 * i + 8) = colors(i + 1)
      }
    }

    vertexPos.toSeq zip vertexCol
  }

}
