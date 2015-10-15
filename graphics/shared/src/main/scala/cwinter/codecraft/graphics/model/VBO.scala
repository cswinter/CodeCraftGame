package cwinter.codecraft.graphics.model

private[graphics] trait VBO {
  val size: Int
  def withSize(size: Int): VBO
  def dispose(gl: Any): Unit
}

private[graphics] object VBO {
  var _count = 0
  def count = _count
}

