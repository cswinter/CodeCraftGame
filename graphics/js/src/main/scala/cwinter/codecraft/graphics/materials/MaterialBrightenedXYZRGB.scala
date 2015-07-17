package cwinter.codecraft.graphics.materials

import cwinter.codecraft.util.CompileTimeLoader
import cwinter.codecraft.util.maths.{ColorRGB, VertexXYZ}
import org.scalajs.dom.raw.{WebGLRenderingContext => GL}


class MaterialBrightenedXYZRGB(implicit gl: GL)
extends JSMaterial[VertexXYZ, ColorRGB, Unit](
  gl = gl,
  vsSource = CompileTimeLoader.loadResource("xyz_rgb_vs.glsl"),
  fsSource = CompileTimeLoader.loadResource("rgb1_brighten_fs.glsl"),
  "vertexPos",
  Some("vertexCol"),
  GL.DEPTH_TEST
)

