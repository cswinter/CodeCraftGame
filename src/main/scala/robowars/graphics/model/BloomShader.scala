package robowars.graphics.model

import javax.media.opengl.GL4
import javax.media.opengl.GL._
import javax.media.opengl._

import robowars.graphics.engine.RenderFrame
import robowars.graphics.matrices.{IdentityMatrix4x4, Matrix4x4}


/*
Notes on framebuffer objects.

A framebuffer objects encapsulates a whole stack of elements required for rendering.
In particular, you can attach textures, render buffers, depth buffer and stencil buffers.

It is not possible to attach the default depth buffer to a framebuffer (source: http://gamedev.stackexchange.com/questions/25495/can-i-use-the-default-depth-buffer-when-drawing-to-fbo)
This means that if we want access to the depth buffer during offscreen drawing,
the whole rendering pipeline must use a custom framebuffer.
The final image can then be copied to the (screen) buffer using a texture blip.


Overview of how glow rendering pass works:
Have one framebuffer which is used for all rendering, and which is eventually drawn to screen.
The FBO consists of:
- texture0: full width, height, used to construct the scene rendered to the screen
- texture1: smaller, temporary storage for horizontal convolution
- texture2: smaller, temporary storage for vertical convolution

The rendering stages (as it concerns glow/opaque materials) are as follows:

1. All opaque materials are drawn to the main texture.
2. All glow materials are drawn to the main texture. They are identified by colour component a=1.
3. Glowing parts are extracted from main texture, convoluted and drawn into texture1.
4. Convolution from texture1 to texture2, this times vertically.
5. texture2 is interpolated (to full size, happens automatically) and added back to texture0

 */
class BloomShader(gl: GL4)
extends Material[VertexXYZ, ColorRGB](
  gl = gl,
  vsPath = "src/main/shaders/xyz_rgb_vs.glsl",
  fsPath = "src/main/shaders/rgb1_fs.glsl",
  "vertexPos",
  Some("vertexCol"),
  GL_DEPTH_TEST
) {
  import gl._   // can ignore error, is just IDEA bug

  override def beforeDraw(projection: Matrix4x4): Unit = {

    // TODO: recompute texel size


    super.beforeDraw(projection)
  }


  val quad =
    new ConcreteModelBuilder[VertexXY, VertexUV](
      Convolution,
      Array(
        (VertexXY( 1.0f,  1.0f), VertexUV(1.0f, 1.0f)),
        (VertexXY( 1.0f, -1.0f), VertexUV(1.0f, 0.0f)),
        (VertexXY(-1.0f, -1.0f), VertexUV(0.0f, 0.0f)),

        (VertexXY( 1.0f,  1.0f), VertexUV(1.0f, 1.0f)),
        (VertexXY(-1.0f, -1.0f), VertexUV(0.0f, 0.0f)),
        (VertexXY(-1.0f,  1.0f), VertexUV(0.0f, 1.0f))
      )
    ).init()
  val quad2 =
    new ConcreteModelBuilder[VertexXY, VertexUV](
      Addition,
      Array(
        (VertexXY( 1.0f,  1.0f), VertexUV(1.0f, 1.0f)),
        (VertexXY( 1.0f, -1.0f), VertexUV(1.0f, 0.0f)),
        (VertexXY(-1.0f, -1.0f), VertexUV(0.0f, 0.0f)),

        (VertexXY( 1.0f,  1.0f), VertexUV(1.0f, 1.0f)),
        (VertexXY(-1.0f, -1.0f), VertexUV(0.0f, 0.0f)),
        (VertexXY(-1.0f,  1.0f), VertexUV(0.0f, 1.0f))
      )
    ).init()

  override def afterDraw(): Unit = {
    super.afterDraw()


    // Horizontal Convolution
    Convolution.orientation = 0
    Convolution.sourceTexture = RenderFrame.fbo.texture0
    Convolution.destTexture = RenderFrame.fbo.texture1
    Convolution.beforeDraw(IdentityMatrix4x4)
    quad.draw()
    Convolution.afterDraw()

    // Vertical Convolution
    Convolution.orientation = 1
    Convolution.sourceTexture = RenderFrame.fbo.texture1
    Convolution.destTexture = RenderFrame.fbo.texture2
    Convolution.beforeDraw(IdentityMatrix4x4)
    quad.draw()
    Convolution.afterDraw()

    // Addition
    Addition.beforeDraw(IdentityMatrix4x4)
    quad2.draw()
    Convolution.afterDraw()


    glDisable(GL_BLEND)
  }


  object Convolution extends Material[VertexXY, VertexUV](
    gl = gl,
    vsPath = "src/main/shaders/texture_xy_vs.glsl",
    fsPath = "src/main/shaders/convolution_fs.glsl",
    "vertexPos",
    Some("texCoords")
  ) {
    val uniformTexelSize = glGetUniformLocation(programID, "texelSize")
    val uniformOrientation = glGetUniformLocation(programID, "orientation")

    var orientation = 0
    var texelSize = VertexXY(1.0f / 1910, 1.0f / 1050)

    var sourceTexture = 0
    var destTexture = 0


    override def beforeDraw(projection: Matrix4x4): Unit = {
      super.beforeDraw(projection)

      //glBlendFunc(GL_ONE, GL_ONE)

      glUniform1i(uniformOrientation, orientation)
      glUniform2f(uniformTexelSize, texelSize.x, texelSize.y)

      // - set texture1 as _destination_ texture
      glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, destTexture, 0)

      glClearColor(0, 0, 0, 0)
      glClear(GL_COLOR_BUFFER_BIT)

      // - set texture0 as _source_ texture
      glActiveTexture(GL_TEXTURE0)
      glBindTexture(GL_TEXTURE_2D, sourceTexture)
    }

    override def afterDraw(): Unit = {
      super.afterDraw()
      glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, RenderFrame.fbo.texture0, 0)
    }
  }

  object Addition extends Material[VertexXY, VertexUV](
    gl = gl,
    vsPath = "src/main/shaders/texture_xy_vs.glsl",
    fsPath = "src/main/shaders/texture_xy_fs.glsl",
    "vertexPos",
    Some("texCoords"),
    GL_BLEND
  ) {

    override def beforeDraw(projection: Matrix4x4): Unit = {
      super.beforeDraw(projection)

      glBlendFunc(GL_ONE_MINUS_DST_ALPHA, GL_ONE)

      // - set texture0 as _destination_ texture
      glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, RenderFrame.fbo.texture0, 0)

      // - set texture1 as _source_ texture
      glActiveTexture(GL_TEXTURE0)
      glBindTexture(GL_TEXTURE_2D, RenderFrame.fbo.texture2)
    }

    override def afterDraw(): Unit = {
      super.afterDraw()
      glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, RenderFrame.fbo.texture0, 0)
    }
  }
}
