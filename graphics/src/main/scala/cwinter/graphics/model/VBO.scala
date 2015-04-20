package cwinter.graphics.model


import javax.media.opengl._


/**
 * Vertex Buffer Object
 */
case class VBO(id: Int, size: Int, vao: Int) {
  def dispose()(implicit gl: GL4): Unit = {
    gl.glDeleteBuffers(1, Array(id), 0)
    gl.glDeleteVertexArrays(1, Array(vao), 0)
    VBO._count -= 1
  }
}


object VBO {
  var _count = 0
  def count = _count
}
