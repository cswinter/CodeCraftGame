package cwinter.graphics.materials

import javax.media.opengl.GL4
import javax.media.opengl.GL._

import cwinter.codinggame.util.maths.{ColorRGBA, VertexXYZ}
import cwinter.graphics.matrices.Matrix4x4

class GaussianGlow(implicit gl: GL4)
  extends Material[VertexXYZ, ColorRGBA, Unit](
    gl = gl,
    vsPath = "graphics/src/main/shaders/xyz_rgba_vs.glsl",
    fsPath = "graphics/src/main/shaders/rgba_gaussian_fs.glsl",
    "vertexPos",
    Some("vertexCol"),
    GL_BLEND
  ) {
  import gl._

  override def beforeDraw(projection: Matrix4x4): Unit = {
    super.beforeDraw(projection)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE)
  }
}
