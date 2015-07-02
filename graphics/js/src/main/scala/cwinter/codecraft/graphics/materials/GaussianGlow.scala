package cwinter.codecraft.graphics.materials

import org.scalajs.dom.raw.{WebGLRenderingContext => GL}
import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.util.maths.{ColorRGBA, VertexXYZ}

class GaussianGlow(implicit gl: GL)
  extends JSMaterial[VertexXYZ, ColorRGBA, Unit](
    gl = gl,
    vsID = "xyz_rgba_vs",
    fsID = "rgba_gaussian_fs",
    "vertexPos",
    Some("vertexCol"),
    GL.BLEND
  ) {

  override def beforeDraw(projection: Matrix4x4): Unit = {
    super.beforeDraw(projection)
    gl.blendFunc(GL.SRC_ALPHA, GL.ONE)
  }
}
