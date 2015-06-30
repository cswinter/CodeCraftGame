package cwinter.codecraft.graphics.model

import org.scalajs.dom.raw.{WebGLBuffer, WebGLRenderingContext => GL}

/**
 * Vertex Buffer Object
 */
case class VBO(id: WebGLBuffer, size: Int, vao: Int) {
  def dispose()(implicit gl: GL): Unit = {
    gl.deleteBuffer(id)
    //gl.deleteVertexArrays(1, Array(vao), 0) TODO: webgl equivalent?
    VBO._count -= 1
  }
}


object VBO {
  var _count = 0
  def count = _count
}

