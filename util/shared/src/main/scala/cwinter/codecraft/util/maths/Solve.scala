package cwinter.codecraft.util.maths

private[codecraft] object Solve {
  /**
   * Returns the smallest strictly positive solution of ax**2 + bx + c.
   * If there is no such solution, returns None
   */
  def quadratic(a: Double, b: Double, c: Double): Option[Double] = {
    val determinant = b * b - 4 * a * c

    if (determinant < 0) return None

    val f = 1.0 / (2 * a)
    val t1 = f * (-b + Math.sqrt(determinant))
    val t2 = f * (-b - Math.sqrt(determinant))

    if (t1 <= 0 && t2 <= 0) None
    else if (t1 <= 0) Some(t2)
    else if (t2 <= 0) Some(t1)
    else Some(Math.min(t1, t2))
  }
}
