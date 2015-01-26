package robowars.graphics.engine

import javax.media.opengl.GL._
import javax.media.opengl.GL4

import robowars.graphics.materials.{RenderToScreen, SimpleMaterial, MaterialXYZRGB, BloomShader}
import robowars.graphics.model._


class RenderStack(implicit val gl: GL4, implicit val fbo: FramebufferObject) {
  import gl._

  // materials
  val SimpleMaterial: SimpleMaterial = new SimpleMaterial
  val MaterialXYRGB: MaterialXYZRGB = new MaterialXYZRGB
  val BloomShader: BloomShader = new BloomShader
  val materials = List(SimpleMaterial, MaterialXYRGB, BloomShader)


  // texture to screen rendering code
  val RenderToScreen: RenderToScreen = new RenderToScreen
  val quad =
    new ConcreteModelBuilder[VertexXY, VertexXY](
      RenderToScreen,
      Array(
        (VertexXY(1.0f, 1.0f), VertexXY(1.0f, 1.0f)),
        (VertexXY(1.0f, -1.0f), VertexXY(1.0f, 0.0f)),
        (VertexXY(-1.0f, -1.0f), VertexXY(0.0f, 0.0f)),

        (VertexXY(1.0f, 1.0f), VertexXY(1.0f, 1.0f)),
        (VertexXY(-1.0f, -1.0f), VertexXY(0.0f, 0.0f)),
        (VertexXY(-1.0f, 1.0f), VertexXY(0.0f, 1.0f))
      )
    ).init()


  def postDraw(camera: Camera2D): Unit = {
    // draw texture to screen
    glViewport(0, 0, camera.screenWidth, camera.screenHeight)
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
    glClear(GL_COLOR_BUFFER_BIT)

    RenderToScreen.beforeDraw(camera.projection)

    glDisable(GL_DEPTH_TEST)
    glBindTexture(GL_TEXTURE_2D, fbo.texture0)

    quad.draw()

    RenderToScreen.afterDraw()
    glBindTexture(GL_TEXTURE_2D, 0)
  }
}
