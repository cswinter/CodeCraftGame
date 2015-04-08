package robowars.graphics.materials

import javax.media.opengl.GL._
import javax.media.opengl.GL4

import robowars.graphics.matrices.Matrix4x4
import robowars.graphics.model.{VBO, ColorRGBA, VertexXYZ}

class GaussianGlowPIntensity(implicit gl: GL4)
  extends Material[VertexXYZ, ColorRGBA, Intensity](
    gl = gl,
    vsPath = "graphics/src/main/shaders/xyz_rgba_vs.glsl",
    fsPath = "graphics/src/main/shaders/rgba_gaussian_pint_fs.glsl",
    "vertexPos",
    Some("vertexCol"),
    GL_BLEND
  ) {
  import gl._
  val uniformIntensity = glGetUniformLocation(programID, "intensity")


  override def beforeDraw(projection: Matrix4x4): Unit = {
    super.beforeDraw(projection)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE)
  }

  override def draw(vbo: VBO, modelview: Matrix4x4): Unit = {
    glUniform1f(uniformIntensity, params.intensity)
    super.draw(vbo, modelview)
  }
}


case class Intensity(intensity: Float) extends AnyVal