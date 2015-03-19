package robowars.graphics.model

/**
 * Vertex Buffer Object
 */
case class VBO(id: Int, size: Int, vao: Int) {
  VBO._count += 1
}


object VBO {
  private var _count = 0
  def count = _count
}
