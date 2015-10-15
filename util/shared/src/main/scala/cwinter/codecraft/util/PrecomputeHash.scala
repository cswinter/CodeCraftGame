package cwinter.codecraft.util


private[cwinter] trait PrecomputeHash {
  private[this] val _hashcode = super.hashCode()
  override def hashCode(): Int = _hashcode
}

