package robowars.graphics.model

import javax.media.opengl.GL4
import javax.media.opengl.GL._

import robowars.graphics.matrices.Matrix4x4


class RenderToScreen(implicit gl: GL4)
extends Material[VertexXY, VertexXY](
  gl = gl,
  vsPath = "src/main/shaders/texture_xy_vs.glsl",
  fsPath = "src/main/shaders/texture_xy_fs.glsl",
  "vertexPos",
  Some("texCoords")
)
