package cwinter.codecraft.graphics.model

import javax.media.opengl._


/**
 * Vertex Buffer Object
 */
private[graphics] case class JVMVBO(id: Int, size: Int, vao: Int) extends VBO {
  def withSize(size: Int): JVMVBO = copy(size = size)
  override def dispose(anyGL: Any): Unit = {
    super.dispose(anyGL)
    assert(anyGL.isInstanceOf[GL4], s"Expected gl of type javax.media.opengl.GL4. Actual: ${anyGL.getClass.getName}")
    val gl = anyGL.asInstanceOf[GL4]
    gl.glDeleteBuffers(1, Array(id), 0)
    gl.glDeleteVertexArrays(1, Array(vao), 0)
    VBO._count -= 1
  }
}

