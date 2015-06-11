package cwinter.codecraft.util.maths

import scala.language.implicitConversions

class Float0To1 private (val value: Float) extends AnyVal

case object Float0To1 {
  def apply(value: Float): Float0To1 = {
    require(value >= 0)
    require(value <= 1)
    new Float0To1(value)
  }

  implicit def float0To1IsFloat(float0To1: Float0To1): Float = {
    float0To1.value
  }
}
