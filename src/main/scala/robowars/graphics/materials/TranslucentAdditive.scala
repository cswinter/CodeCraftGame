package robowars.graphics.materials

import javax.media.opengl.GL._
import javax.media.opengl.GL4

import robowars.graphics.matrices.Matrix4x4
import robowars.graphics.model.{VertexXYZ, ColorRGBA}


class TranslucentAdditive(implicit gl: GL4)
  extends Material[VertexXYZ, ColorRGBA](
    gl = gl,
    vsPath = "src/main/shaders/xyz_rgba_vs.glsl",
    fsPath = "src/main/shaders/rgba_fs.glsl",
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
