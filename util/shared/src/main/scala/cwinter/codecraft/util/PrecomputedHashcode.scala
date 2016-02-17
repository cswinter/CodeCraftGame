package cwinter.codecraft.util

import scala.runtime.ScalaRunTime


private[cwinter] trait PrecomputedHashcode {
  self: Product =>
  override lazy val hashCode: Int = ScalaRunTime._hashCode(this)
}

