package robowars.graphics.model

import javax.media.opengl.GL4


class SimpleMaterial(gl: GL4)
extends Material[VertexXY, EmptyVertex.type](
  gl = gl,
  vsPath = "src/main/shaders/vs_basic.glsl",
  fsPath = "src/main/shaders/fs_basic.glsl",
  "vertexPos"
)
