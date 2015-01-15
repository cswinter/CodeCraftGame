package robowars.graphics.model

import javax.media.opengl.GL4


class MaterialXYRGB(gl: GL4)
extends Material[VertexXY, ColorRGB](
  gl = gl,
  vsPath = "src/main/shaders/xy_rgb_vs.glsl",
  fsPath = "src/main/shaders/rgb_fs.glsl",
  "vertexPos",
  Some("vertexCol")
)
