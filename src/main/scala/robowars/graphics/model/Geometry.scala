package robowars.graphics.model


object Geometry {
  import math._

  def polygonVertices(
    n: Int,
    orientation: Float = 0,
    radius: Float = 1,
    position: VertexXY = NullVectorXY
  ): Seq[VertexXY] = {
    assert(n >= 3)
    assert(radius > 0)
    for (i <- 0 until n)
    yield radius * VertexXY(i * 2 * math.Pi.toFloat / n + orientation) + position
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
