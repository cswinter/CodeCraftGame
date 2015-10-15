package cwinter.codecraft.graphics.materials

import cwinter.codecraft.graphics.model.VBO
import cwinter.codecraft.util.CompileTimeLoader
import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.util.maths.{ColorRGBA, VertexXYZ}
import org.scalajs.dom.raw.{WebGLRenderingContext => GL}

private[graphics] class GaussianGlowPIntensity(implicit gl: GL)
  extends JSMaterial[VertexXYZ, ColorRGBA, Intensity](
    gl = gl,
    vsSource = CompileTimeLoader.loadResource("xyz_rgba_vs.glsl"),
    fsSource = CompileTimeLoader.loadResource("rgba_gaussian_pint_fs.glsl"),
    "vertexPos",
    Some("vertexCol"),
    GL.BLEND
  ) {
  val uniformIntensity = gl.getUniformLocation(programID, "intensity")

  override def beforeDraw(projection: Matrix4x4): Unit = {
    super.beforeDraw(projection)
    gl.blendFunc(GL.SRC_ALPHA, GL.ONE)
  }

  override def draw(vbo: VBO, modelview: Matrix4x4): Unit = {
    gl.uniform1f(uniformIntensity, params.intensity)
    super.draw(vbo, modelview)
  }
}
