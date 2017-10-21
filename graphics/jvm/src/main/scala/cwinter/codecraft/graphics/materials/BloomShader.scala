package cwinter.codecraft.graphics.materials

import com.jogamp.opengl.GL._
import com.jogamp.opengl.GL4

import cwinter.codecraft.graphics.engine.{RenderFrame, FramebufferObject}
import cwinter.codecraft.util.maths.matrices.{Matrix4x4, IdentityMatrix4x4}
import cwinter.codecraft.graphics.model.{VBO, JVMVBO$}
import cwinter.codecraft.util.maths.{ColorRGB, VertexXYZ, VertexUV, VertexXY}


/*
Notes on framebuffer objects.

A framebuffer object encapsulates a whole stack of elements required for rendering.
In particular, you can attach textures, render buffers, depth buffer and stencil buffers.

It is not possible to attach the default depth buffer to a framebuffer
(source: http://gamedev.stackexchange.com/questions/25495/can-i-use-the-default-depth-buffer-when-drawing-to-fbo)
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
private[graphics] class BloomShader(implicit gl: GL4, fbo: FramebufferObject)
  extends JVMMaterial[VertexXYZ, ColorRGB, Unit](
    gl = gl,
    vsPath = "xyz_rgb_vs.glsl",
    fsPath = "rgb1_fs.glsl",
    "vertexPos",
    Some("vertexCol"),
    GL_DEPTH_TEST
  ) {

  import fbo.{texture0 => mainTexture, texture1 => tmpTexture1, texture2 => tmpTexture2}
  import gl._


  override def afterDraw(): Unit = {
    super.afterDraw()

    // Horizontal Convolution
    glViewport(0, 0, fbo.width, fbo.height)
    HConvolution.beforeDraw(IdentityMatrix4x4)
    HConvolution.draw(hconvQuad, IdentityMatrix4x4)
    HConvolution.afterDraw()

    // Vertical Convolution
    VConvolution.beforeDraw(IdentityMatrix4x4)
    HConvolution.draw(vconvQuad, IdentityMatrix4x4)
    VConvolution.afterDraw()

    // Addition
    glViewport(0, 0, 2 * fbo.width, 2 * fbo.height)
    Addition.beforeDraw(IdentityMatrix4x4)
    Addition.draw(addQuad, IdentityMatrix4x4)
    Addition.afterDraw()

    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, mainTexture, 0)
  }


  private abstract class Convolution(val orientation: Int, val sourceTexture: () => Int, val destTexture: () => Int)
    extends JVMMaterial[VertexXY, VertexUV, Unit](
      gl = gl,
      vsPath = "texture_xy_vs.glsl",
      fsPath = "convolution_fs.glsl",
      "vertexPos",
      Some("texCoords")) {

    val uniformTexelSize = glGetUniformLocation(programID, "texelSize")
    val uniformOrientation = glGetUniformLocation(programID, "orientation")


    override def beforeDraw(projection: Matrix4x4): Unit = {
      super.beforeDraw(projection)

      val texelSize = VertexXY(1.0f / fbo.width, 1.0f / fbo.height)
      glUniform1i(uniformOrientation, orientation)
      glUniform2f(uniformTexelSize, texelSize.x, texelSize.y)

      // - set texture1 as _destination_ texture
      glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, destTexture(), 0)

      glClearColor(0, 0, 0, 0)
      glClear(GL_COLOR_BUFFER_BIT)

      // - set texture0 as _source_ texture
      glActiveTexture(GL_TEXTURE0)
      glBindTexture(GL_TEXTURE_2D, sourceTexture())
    }
  }

  private object HConvolution extends Convolution(0, () => mainTexture, () => tmpTexture1)

  private object VConvolution extends Convolution(1, () => tmpTexture1, () => tmpTexture2)

  // TODO: make use of parameters?
  private object Addition extends JVMMaterial[VertexXY, VertexUV, Unit](
    gl = gl,
    vsPath = "texture_xy_vs.glsl",
    fsPath = "texture_xy_fs.glsl",
    "vertexPos",
    Some("texCoords"),
    GL_BLEND
  ) {

    override def beforeDraw(projection: Matrix4x4): Unit = {
      super.beforeDraw(projection)

      glBlendFunc(GL_ONE, GL_ONE)

      // - set texture0 as _destination_ texture
      glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, fbo.texture0, 0)

      // - set texture1 as _source_ texture
      glActiveTexture(GL_TEXTURE0)
      glBindTexture(GL_TEXTURE_2D, fbo.texture2)
    }

    override def afterDraw(): Unit = {
      super.afterDraw()
      glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, fbo.texture0, 0)
    }
  }

  private val hconvQuad = genFullsizeQuad(HConvolution)
  private val vconvQuad = genFullsizeQuad(VConvolution)
  private val addQuad = genFullsizeQuad(Addition)

  def genFullsizeQuad(material: Material[VertexXY, VertexUV, Unit]): VBO = {
    material.createVBO(
      Array(
        (VertexXY(1.0f, 1.0f), VertexUV(1.0f, 1.0f)),
        (VertexXY(-1.0f, -1.0f), VertexUV(0.0f, 0.0f)),
        (VertexXY(1.0f, -1.0f), VertexUV(1.0f, 0.0f)),

        (VertexXY(1.0f, 1.0f), VertexUV(1.0f, 1.0f)),
        (VertexXY(-1.0f, 1.0f), VertexUV(0.0f, 1.0f)),
        (VertexXY(-1.0f, -1.0f), VertexUV(0.0f, 0.0f))
      )
    )
  }


  override def dispose(): Unit = {
    super.dispose()
    VConvolution.dispose()
    HConvolution.dispose()
    hconvQuad.dispose(gl)
    vconvQuad.dispose(gl)
    addQuad.dispose(gl)
  }
}

