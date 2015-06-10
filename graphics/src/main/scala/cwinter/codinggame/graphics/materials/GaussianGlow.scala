package cwinter.codinggame.graphics.materials

import javax.media.opengl.GL4
import javax.media.opengl.GL._

import cwinter.codinggame.graphics.matrices.Matrix4x4
import cwinter.codinggame.util.maths.{ColorRGBA, VertexXYZ}

class GaussianGlow(implicit gl: GL4)
  extends Material[VertexXYZ, ColorRGBA, Unit](
    gl = gl,
    vsPath = "xyz_rgba_vs.glsl",
    fsPath = "rgba_gaussian_fs.glsl",
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
