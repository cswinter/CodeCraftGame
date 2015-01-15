package robowars.graphics.engine

import javax.media.opengl.GL._
import javax.media.opengl.GL2ES3._
import javax.media.opengl.GL4


class FramebufferObject(width: Int, height: Int, gl: GL4) {
  import gl._
  private[this] val intRef = new Array[Int](1)



  // generate and bind fbo
  glGenFramebuffers(1, intRef, 0)
  val fbo = intRef(0)
  glBindFramebuffer(GL_FRAMEBUFFER, fbo)

  // generate and attach texture
  val texture0 = genTexture(width, height)
  glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture0, 0)

  // create and attach depth + stencil renderbuffer
  glGenRenderbuffers(1, intRef, 0)
  val rbo = intRef(0)
  glBindRenderbuffer(GL_RENDERBUFFER, rbo)
  glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height)
  glBindRenderbuffer(GL_RENDERBUFFER, 0)

  glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo)


  assert(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE, "Incomplete Framebuffer")
  glBindFramebuffer(GL_FRAMEBUFFER, 0)



  def delete(): Unit = {
    glDeleteFramebuffers(1, Array(fbo), 0)
  }

  def genTexture(width: Int, height: Int): Int = {
    glGenTextures(1, intRef, 0)
    val texture = intRef(0)
    glBindTexture(GL_TEXTURE_2D, texture)

    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_FLOAT, null)

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

    glBindTexture(GL_TEXTURE_2D, 0)

    texture
  }
}
