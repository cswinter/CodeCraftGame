package cwinter.codecraft.graphics.materials

import cwinter.codecraft.util.maths.{ColorRGB, VertexXYZ}
import org.scalajs.dom.raw.{WebGLRenderingContext => GL}


class MaterialXYZRGB(implicit gl: GL)
extends JSMaterial[VertexXYZ, ColorRGB, Unit](
  gl = gl,
  vsID = "xyz_rgb_vs",
  fsID = "rgb1_fs",
  "vertexPos",
  Some("vertexCol"),
  GL.DEPTH_TEST
)

