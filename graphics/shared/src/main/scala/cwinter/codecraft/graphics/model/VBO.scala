package cwinter.codecraft.graphics.model


private[graphics] trait VBO {
  val size: Int
  private[this] var _disposed = false
  def disposed: Boolean = _disposed
  def withSize(size: Int): VBO
  def dispose(gl: Any): Unit = _disposed = true
}

private[graphics] object VBO {
  var _count = 0
  def count = _count
}

