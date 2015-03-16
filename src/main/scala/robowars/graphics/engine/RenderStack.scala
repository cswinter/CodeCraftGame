package robowars.graphics.engine

import javax.media.opengl.GL._
import javax.media.opengl.GL4

import robowars.graphics.materials._
import robowars.graphics.matrices.IdentityMatrix4x4
import robowars.graphics.model._


class RenderStack(implicit val gl: GL4, implicit val fbo: FramebufferObject) {
  import gl._

  // materials
  val SimpleMaterial: SimpleMaterial = new SimpleMaterial
  val MaterialXYRGB: MaterialXYZRGB = new MaterialXYZRGB
  val BloomShader: BloomShader = new BloomShader
  val GaussianGlow: GaussianGlow = new GaussianGlow
  val GaussianGlowPIntensity: GaussianGlowPIntensity = new GaussianGlowPIntensity
  val TranslucentAdditive: TranslucentAdditive = new TranslucentAdditive
  val materials = List(SimpleMaterial, MaterialXYRGB, BloomShader, GaussianGlow, GaussianGlowPIntensity, TranslucentAdditive)


  // texture to screen rendering code
  val RenderToScreen: RenderToScreen = new RenderToScreen
  val quad = RenderToScreen.createVBO(
    Array(
      (VertexXY(1.0f, 1.0f), VertexXY(1.0f, 1.0f)),
      (VertexXY(-1.0f, -1.0f), VertexXY(0.0f, 0.0f)),
      (VertexXY(1.0f, -1.0f), VertexXY(1.0f, 0.0f)),

      (VertexXY(1.0f, 1.0f), VertexXY(1.0f, 1.0f)),
      (VertexXY(-1.0f, 1.0f), VertexXY(0.0f, 1.0f)),
      (VertexXY(-1.0f, -1.0f), VertexXY(0.0f, 0.0f))
    )
  )


  def postDraw(camera: Camera2D): Unit = {
    // draw texture to screen
    glViewport(0, 0, camera.screenWidth, camera.screenHeight)
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
    glClear(GL_COLOR_BUFFER_BIT)

    RenderToScreen.beforeDraw(camera.projection)

    glDisable(GL_DEPTH_TEST)
    glBindTexture(GL_TEXTURE_2D, fbo.texture0)

    RenderToScreen.draw(quad, IdentityMatrix4x4)

    RenderToScreen.afterDraw()
    glBindTexture(GL_TEXTURE_2D, 0)
  }
}

