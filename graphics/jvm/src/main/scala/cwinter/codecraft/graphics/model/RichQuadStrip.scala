package cwinter.codecraft.graphics.model

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.maths.{VertexXYZ, VertexXY, Vertex}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag


case class RichQuadStrip[TColor <: Vertex : ClassTag, TParams](
  material: Material[VertexXYZ, TColor, TParams],
  midpoints: Seq[VertexXY],
  colorsInside: Seq[TColor],
  colorsOutside: Seq[TColor],
  width: Float,
  zPos: Float = 0
) extends PrimitiveModelBuilder[RichQuadStrip[TColor, TParams], TColor, TParams]{
  val shape: RichQuadStrip[TColor, TParams] = this
  val n = midpoints.length

  assert(colorsInside.length == n)
  assert(colorsOutside.length == n)
  assert(n >= 2, "There must be at least two midpoints.")

  protected def computeVertexData(): Seq[(VertexXYZ, TColor)] = {
    // diagram: https://www.dropbox.com/sc/owb97vdjnl7bxq0/AAAg0qFJNR5lyxoB4RG7OLJ6a

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
    val stride = 21
    val vertexPos = new ArrayBuffer[VertexXYZ]((n - 1) * stride - 9)
    for (i <- 0 until n -1)
    {
      val midpointStart = (0.5f * (leftStart(i) + rightStart(i))).zPos(zPos)
      val midpointEnd = (0.5f * (leftEnd(i) + rightEnd(i))).zPos(zPos)

      // quad from point i to point i + 1 (left side)
      vertexPos += leftStart(i).zPos(zPos)
      vertexPos += midpointStart
      vertexPos += midpointEnd

      vertexPos += leftStart(i).zPos(zPos)
      vertexPos += midpointEnd
      vertexPos += leftEnd(i).zPos(zPos)

      // quad from point i to point i + 1 (right side)
      vertexPos += midpointStart
      vertexPos += rightStart(i).zPos(zPos)
      vertexPos += midpointEnd

      vertexPos += rightStart(i).zPos(zPos)
      vertexPos += rightEnd(i).zPos(zPos)
      vertexPos += midpointEnd


      // fill-in at point i
      if (i < n - 2) {
        val midpointStartNext = (0.5f * (leftStart(i + 1) + rightStart(i + 1))).zPos(zPos)
        if (leftEnd(i) == leftStart(i + 1)) {
          vertexPos += leftStart(i + 1).zPos(zPos)
          vertexPos += midpointEnd
          vertexPos += midpointStartNext

          vertexPos += midpointStartNext
          vertexPos += midpointEnd
          vertexPos += rightEnd(i).zPos(zPos)

          vertexPos += midpointStartNext
          vertexPos += rightEnd(i).zPos(zPos)
          vertexPos += rightStart(i + 1).zPos(zPos)
          // enabling check increases efficiency, but makes midpoint coloring awkward
        } else { // if (rightStart(i) == rightEnd(i - 1)) {
          vertexPos += rightStart(i + 1).zPos(zPos)
          vertexPos += midpointStartNext
          vertexPos += midpointEnd

          vertexPos += midpointEnd
          vertexPos += midpointStartNext
          vertexPos += leftEnd(i).zPos(zPos)

          vertexPos += midpointStartNext
          vertexPos += leftStart(i + 1).zPos(zPos)
          vertexPos += leftEnd(i).zPos(zPos)
        }
      }
    }


    val vertexCol = new Array[TColor](stride * (n - 1) - 9)
    for (i <- 0 until n - 1) {
      vertexCol(stride * i + 0) = colorsOutside(i)    // start
      vertexCol(stride * i + 1) = colorsInside(i)     // midpoint start
      vertexCol(stride * i + 2) = colorsInside(i + 1) // midpontEnd
      vertexCol(stride * i + 3) = colorsOutside(i)    // start
      vertexCol(stride * i + 4) = colorsInside(i + 1) // midpointEnd
      vertexCol(stride * i + 5) = colorsOutside(i + 1)//end

      vertexCol(stride * i + 6) = colorsInside(i)        // midpoint start
      vertexCol(stride * i + 7) = colorsOutside(i)       // start
      vertexCol(stride * i + 8) = colorsInside(i + 1)   // midpoint end
      vertexCol(stride * i + 9) = colorsOutside(i)        // start
      vertexCol(stride * i + 10) = colorsOutside(i + 1)  // end
      vertexCol(stride * i + 11) = colorsInside(i + 1)   // midpointEnd

      if (i < n - 2) {
        vertexCol(stride * i + 12) = colorsOutside(i + 1)
        vertexCol(stride * i + 13) = colorsInside(i + 1)
        vertexCol(stride * i + 14) = colorsInside(i + 1)

        vertexCol(stride * i + 15) = colorsInside(i + 1)
        vertexCol(stride * i + 16) = colorsInside(i + 1)
        vertexCol(stride * i + 17) = colorsOutside(i + 1)

        vertexCol(stride * i + 18) = colorsInside(i + 1)
        vertexCol(stride * i + 19) = colorsOutside(i + 1)
        vertexCol(stride * i + 20) = colorsOutside(i + 1)
      }
    }

    assert(vertexPos.toArray.length == stride * (n - 1) - 9)
    vertexPos.toSeq zip vertexCol
  }

}
