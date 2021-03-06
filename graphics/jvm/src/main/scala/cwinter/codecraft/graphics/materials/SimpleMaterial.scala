package cwinter.codecraft.graphics.materials

import com.jogamp.opengl.GL._
import com.jogamp.opengl.GL4

import cwinter.codecraft.util.maths.{EmptyVertex, VertexXY}


private[graphics] class SimpleMaterial(implicit gl: GL4)
extends JVMMaterial[VertexXY, EmptyVertex.type, Unit](
  gl = gl,
  vsPath = "basic_vs.glsl",
  fsPath = "basic_fs.glsl",
  "vertexPos",
  None,
  GL_DEPTH_TEST
)
