package cwinter.codecraft.util.maths

final case class Rectangle(xMin: Double, xMax: Double, yMin: Double, yMax: Double) {
  assert(xMin < xMax)
  assert(yMin < yMax)


  def contains(point: Vector2): Boolean =
    point.x >= xMin && point.x <= xMax && point.y >= yMin && point.y <= yMax


  def width: Double = xMax - xMin
  def height: Double = yMax - yMin
}
