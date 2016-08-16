package cwinter.codecraft.graphics.materials


import com.jogamp.opengl.GL4
import cwinter.codecraft.util.maths.VertexXY


private[graphics] class RenderToScreen(implicit gl: GL4)
extends JVMMaterial[VertexXY, VertexXY, Unit](
  gl = gl,
  vsPath = "texture_xy_vs.glsl",
  fsPath = "texture_xy_fs.glsl",
  "vertexPos",
  Some("texCoords")
)
