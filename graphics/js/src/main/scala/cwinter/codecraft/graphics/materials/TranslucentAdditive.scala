package cwinter.codecraft.graphics.materials

import cwinter.codecraft.util.CompileTimeLoader
import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.util.maths.{ColorRGBA, VertexXYZ}
import org.scalajs.dom.raw.{WebGLRenderingContext => GL}


class TranslucentAdditive(implicit gl: GL)
  extends JSMaterial[VertexXYZ, ColorRGBA, Unit](
    gl = gl,
    vsSource = CompileTimeLoader.loadResource("xyz_rgba_vs.glsl"),
    fsSource = CompileTimeLoader.loadResource("rgba_fs.glsl"),
    "vertexPos",
    Some("vertexCol"),
    GL.BLEND
  ) {

  override def beforeDraw(projection: Matrix4x4): Unit = {
    super.beforeDraw(projection)
    gl.blendFunc(GL.SRC_ALPHA, GL.ONE)
  }
}
