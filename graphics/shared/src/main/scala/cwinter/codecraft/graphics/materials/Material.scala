package cwinter.codecraft.graphics.materials

import cwinter.codecraft.graphics.model.VBO
import cwinter.codecraft.util.maths.Vertex
import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] trait Material[TPosition <: Vertex, TColor <: Vertex, TParams] {
  var params: TParams = _

  def beforeDraw(projection: Matrix4x4): Unit
  def draw(vbo: VBO, modelview: Matrix4x4): Unit
  def afterDraw(): Unit
  def createVBO(vertexData: Seq[(TPosition, TColor)], dynamic: Boolean = false): VBO
}


private[graphics] object Material {
  private[materials] var _drawCalls = 0

  def resetDrawCalls(): Unit = _drawCalls = 0
  def drawCalls = _drawCalls
}

