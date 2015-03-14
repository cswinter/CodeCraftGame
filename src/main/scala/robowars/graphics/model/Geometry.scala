package robowars.graphics.model


object Geometry {
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
}
