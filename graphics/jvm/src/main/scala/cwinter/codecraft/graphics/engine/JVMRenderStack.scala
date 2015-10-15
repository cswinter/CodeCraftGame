package cwinter.codecraft.graphics.engine

import javax.media.opengl.GL._
import javax.media.opengl.GL4

import cwinter.codecraft.graphics.materials._
import cwinter.codecraft.util.maths.matrices.IdentityMatrix4x4
import cwinter.codecraft.util.maths.VertexXY


private[graphics] class JVMRenderStack(implicit val gl: GL4, implicit val fbo: FramebufferObject) extends RenderStack {
  import gl._

  // materials
  val SimpleMaterial: SimpleMaterial = new SimpleMaterial
  val MaterialXYZRGB: MaterialXYZRGB = new MaterialXYZRGB
  val BloomShader: BloomShader = new BloomShader
  val GaussianGlow: GaussianGlow = new GaussianGlow
  val GaussianGlowPIntensity: GaussianGlowPIntensity = new GaussianGlowPIntensity
  val TranslucentAdditive: TranslucentAdditive = new TranslucentAdditive
  val TranslucentProportional: TranslucentProportional = new TranslucentProportional
  val TranslucentAdditivePIntensity: TranslucentAdditivePIntensity = new TranslucentAdditivePIntensity
  // TODO: possible to write macro that automatically collects all materials/emits warning if not all are added?
  /** NEED TO ADD ANY NEW MATERIALS HERE: **/


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

