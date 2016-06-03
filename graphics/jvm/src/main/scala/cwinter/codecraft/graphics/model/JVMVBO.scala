package cwinter.codecraft.graphics.model

import com.jogamp.opengl._

/**
  * Vertex Buffer Object
  */
private[graphics] case class JVMVBO(id: Int, size: Int, vao: Int) extends VBO {
  def withSize(size: Int): JVMVBO = copy(size = size)
  override def dispose(anyGL: Any): Unit = {
    super.dispose(anyGL)
    anyGL match {
      case gl2: GL2 =>
        gl2.glDeleteBuffers(1, Array(id), 0)
        gl2.glDeleteVertexArrays(1, Array(vao), 0)
      case gl4: GL4 =>
        gl4.glDeleteBuffers(1, Array(id), 0)
        gl4.glDeleteVertexArrays(1, Array(vao), 0)
      case _ =>
        throw new Exception(
            s"Expected gl of type javax.media.opengl.GL2 or javax.media.opengl.GL4. Actual: ${anyGL.getClass.getName}")
    }
    VBO._count -= 1
  }
}
