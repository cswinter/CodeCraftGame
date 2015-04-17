package cwinter.codinggame.maths

final case class Rectangle(xMin: Double, xMax: Double, yMin: Double, yMax: Double) {
  assert(xMin < xMax)
  assert(yMin < yMax)
}
