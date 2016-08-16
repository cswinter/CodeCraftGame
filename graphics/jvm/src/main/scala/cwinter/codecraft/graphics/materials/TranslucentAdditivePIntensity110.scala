package cwinter.codecraft.graphics.materials

import com.jogamp.opengl.GL._
import com.jogamp.opengl.GL2

import cwinter.codecraft.graphics.model.VBO
import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.util.maths.{ColorRGBA, VertexXYZ}


private[graphics] class TranslucentAdditivePIntensity110(implicit gl: GL2)
  extends JVMMaterial[VertexXYZ, ColorRGBA, Intensity](
    gl = gl,
    vsPath = "110_xyz_rgba_vs.glsl",
    fsPath = "110_rgba_pint_fs.glsl",
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
