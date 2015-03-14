package robowars.graphics.materials

import javax.media.opengl.GL4

import robowars.graphics.model.VertexXY


class RenderToScreen(implicit gl: GL4)
extends Material[VertexXY, VertexXY, Unit](
  gl = gl,
  vsPath = "src/main/shaders/texture_xy_vs.glsl",
  fsPath = "src/main/shaders/texture_xy_fs.glsl",
  "vertexPos",
  Some("texCoords")
)
