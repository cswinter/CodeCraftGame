package cwinter.codecraft.graphics.materials

import javax.media.opengl.GL4

import cwinter.codecraft.util.maths.VertexXY


class RenderToScreen(implicit gl: GL4)
extends Material[VertexXY, VertexXY, Unit](
  gl = gl,
  vsPath = "texture_xy_vs.glsl",
  fsPath = "texture_xy_fs.glsl",
  "vertexPos",
  Some("texCoords")
)
