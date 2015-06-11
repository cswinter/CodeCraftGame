package cwinter.codecraft.graphics.materials

import javax.media.opengl.GL._
import javax.media.opengl.GL4

import cwinter.codecraft.util.maths.{ColorRGB, VertexXYZ}


class MaterialXYZRGB(implicit gl: GL4)
extends Material[VertexXYZ, ColorRGB, Unit](
  gl = gl,
  vsPath = "xyz_rgb_vs.glsl",
  fsPath = "rgb0_fs.glsl",
  "vertexPos",
  Some("vertexCol"),
  GL_DEPTH_TEST
)
