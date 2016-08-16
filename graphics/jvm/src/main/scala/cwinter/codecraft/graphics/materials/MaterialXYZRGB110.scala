package cwinter.codecraft.graphics.materials

import com.jogamp.opengl.GL._
import com.jogamp.opengl.GL2

import cwinter.codecraft.util.maths.{ColorRGB, VertexXYZ}


private[graphics] class MaterialXYZRGB110(implicit gl: GL2)
extends JVMMaterial[VertexXYZ, ColorRGB, Unit](
  gl = gl,
  vsPath = "110_xyz_rgb_vs.glsl",
  fsPath = "110_rgb1_fs.glsl",
  "vertexPos",
  Some("vertexCol"),
  GL_DEPTH_TEST
)
