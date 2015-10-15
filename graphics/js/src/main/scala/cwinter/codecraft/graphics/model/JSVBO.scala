package cwinter.codecraft.graphics.model

import org.scalajs.dom.raw.{WebGLBuffer, WebGLRenderingContext => GL}

/**
 * Vertex Buffer Object
 */
private[graphics] case class JSVBO(id: WebGLBuffer, size: Int) extends VBO {
  def withSize(size: Int): JSVBO = copy(size = size)
  def dispose(anyGL: Any): Unit = {
    assert(anyGL.isInstanceOf[GL], s"Expected gl of type ${GL.getClass.getName}. Actual: ${anyGL.getClass.getName}")
    val gl = anyGL.asInstanceOf[GL]
    gl.deleteBuffer(id)
    VBO._count -= 1
  }
}

