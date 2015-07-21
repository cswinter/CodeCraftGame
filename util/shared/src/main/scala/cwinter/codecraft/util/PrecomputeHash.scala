package cwinter.codecraft.util


trait PrecomputeHash {
  private[this] val _hashcode = super.hashCode()
  override def hashCode(): Int = _hashcode
}

