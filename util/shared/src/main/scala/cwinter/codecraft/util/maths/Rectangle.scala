package cwinter.codecraft.util.maths

/**
 * Defines an axis aligned rectangle positioned in 2D space.
 * @param xMin The x coordinate for the left side of the rectangle.
 * @param xMax The x coordinate for the right hand side of the rectangle.
 * @param yMin The y coordinate of the bottom side of the rectangle.
 * @param yMax The y coordinate of the top side of the rectangle.
 */
final case class Rectangle(xMin: Double, xMax: Double, yMin: Double, yMax: Double) {
  assert(xMin < xMax)
  assert(yMin < yMax)


  /**
   * Returns true if `point` is inside the rectangle.
   */
  def contains(point: Vector2): Boolean =
    point.x >= xMin && point.x <= xMax && point.y >= yMin && point.y <= yMax


  /**
   * Returns the width.
   */
  def width: Double = xMax - xMin

  /**
   * Returns the height.
   */
  def height: Double = yMax - yMin

  /**
   * Returns true if this rectangle and `that` overlap.
   */
  def intersects(that: Rectangle): Boolean = !(
      this.xMin > that.xMax ||
      this.xMax < that.xMin ||
      this.yMin > that.yMax ||
      this.yMax < that.yMin
    )
}


private[codecraft] object Rectangle {
  private final val RectangleRegex = """Rectangle\((.*),(.*),(.*),(.*)\)""".r
  def fromString(string: String): Rectangle = {
    val RectangleRegex(xMin, xMax, yMin, yMax) = string
    Rectangle(xMin.toDouble, xMax.toDouble, yMin.toDouble, yMax.toDouble)
  }
}
