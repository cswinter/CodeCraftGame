package cwinter.codecraft.util.maths

import scala.math._

private[codecraft] object Functions {
  def gaussian(x: Double): Double =
    sqrt(2 * Pi) * exp(-x * x)
}
