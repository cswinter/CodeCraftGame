package robowars.graphics.engine

import javax.media.opengl.GL._
import javax.media.opengl.GL2ES3._
import javax.media.opengl.GL4


class FramebufferObject(implicit val gl: GL4) {
  import gl._
  private[this] val intRef = new Array[Int](1)

  // generate and bind fbo
  var fbo: Int = 0

  var texture0: Int = -42
  var texture1: Int = 0
  var texture2: Int = 0
  var depthBuffer: Int = 0

  def resize(width: Int, height: Int)(gl: GL4) {
    //if (texture0 != -42) disposeTextures()
    glGenFramebuffers(1, intRef, 0)
    fbo = intRef(0)
    glBindFramebuffer(GL_FRAMEBUFFER, fbo)

    glBindFramebuffer(GL_FRAMEBUFFER, fbo)

    // generate and attach texture
    texture0 = genTexture(width, height)
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture0, 0)

    texture1 = genTexture(width, height)
    texture2 = genTexture(width, height)

    // create and attach depth + stencil renderbuffer
    glGenRenderbuffers(1, intRef, 0)
    depthBuffer = intRef(0)
    glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer)
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT32F, width, height)
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer)
    glBindRenderbuffer(GL_RENDERBUFFER, 0)


    assert(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE, "Incomplete Framebuffer")
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
  }

  private def disposeTextures(): Unit = {
    glDeleteTextures(4, Array(texture0, texture1, texture2, depthBuffer), 0)
  }


  def dispose(): Unit = {
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
