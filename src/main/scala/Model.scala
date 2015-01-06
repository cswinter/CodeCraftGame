import javax.media.opengl._
import javax.media.opengl.GL._

import com.jogamp.common.nio.Buffers


class Model(val material: Material, vertices: Array[Float])(implicit gl: GL4) {
  import gl._

  val vbo = createVBO(vertices)

  def draw(): Unit = {
    material.draw(vbo)
  }


  private def createVBO(vertexData: Array[Float]): Int = {
    // create vbo handle
    val vboRef = new Array[Int](1)
    glGenBuffers(1, vboRef, 0)
    val vbo = vboRef(0)

    // store data to GPU
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    val numBytes = vertexData.length * 4
    val verticesBuffer = Buffers.newDirectFloatBuffer(vertexData)
    glBufferData(GL_ARRAY_BUFFER, numBytes, verticesBuffer, GL.GL_STATIC_DRAW)

    // bind shader attributes (input parameters)
    material.bindAttributes(vbo)
    glBindBuffer(GL_ARRAY_BUFFER, 0)

    vbo
  }
}
