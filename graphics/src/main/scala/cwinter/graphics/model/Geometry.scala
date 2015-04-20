package cwinter.graphics.model


object Geometry {
  import math._

  def polygonVertices(
    n: Int,
    orientation: Float = 0,
    radius: Float = 1,
    position: VertexXY = NullVectorXY
  ): IndexedSeq[VertexXY] = {

    assert(n >= 3)
    assert(radius > 0)

    for (i <- 0 until n)
    yield radius * VertexXY(i * 2 * math.Pi.toFloat / n + orientation) + position
  }

  def polygonVertices2(
    n: Int,
    orientation: Float = 0,
    radius: Float = 1,
    position: VertexXY = NullVectorXY
  ): IndexedSeq[VertexXY] = {
    val angle0 = (Pi + math.Pi / n).toFloat
    polygonVertices(n, orientation + angle0, radius, position)
  }


  /**
   * Computes the inradius of a regular polygon given the radius.
   * @param radius The radius.
   */
  def inradius(radius: Float, n: Int): Float =
    radius * cos(Pi / n).toFloat

  /**
   * Computes the circumradius of a regular polygon given the inradius.
   * @param inradius The inradius.
   */
  def circumradius(inradius: Float, n: Int): Float =
    inradius / cos(Pi / n).toFloat
}
