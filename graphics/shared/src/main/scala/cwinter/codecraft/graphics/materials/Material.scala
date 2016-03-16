package cwinter.codecraft.graphics.materials

import cwinter.codecraft.graphics.model.VBO
import cwinter.codecraft.util.maths.Vertex
import cwinter.codecraft.util.maths.matrices.Matrix4x4


private[graphics] trait Material[TPosition <: Vertex, TColor <: Vertex, TParams] {
  var params: TParams = _

  def beforeDraw(projection: Matrix4x4): Unit
  def draw(vbo: VBO, modelview: Matrix4x4): Unit
  def afterDraw(): Unit

  protected def createVBO(vertexData: Array[Float], dynamic: Boolean): VBO
  def nCompPos: Int
  def nCompCol: Int


  def createVBO(vertexData: Seq[(TPosition, TColor)], dynamic: Boolean = false): VBO = {
    val nComponents = nCompPos + nCompCol
    val data = new Array[Float](nComponents * vertexData.size)
    var i = 0
    for ((pos, col) <- vertexData) {
      for (j <- 0 until nCompPos) {
        data(i * nComponents + j) = pos(j)
      }
      for (j <- 0 until nCompCol) {
        assert(col != null)
        data(i * nComponents + j + nCompPos) = col(j)
      }
      i += 1
    }

    createVBO(data, dynamic)
  }

  def createVBOSeq(vertexDataSeqs: Seq[Seq[(TPosition, TColor)]], dynamic: Boolean = false): VBO = {
    val nComponents = nCompPos + nCompCol
    val nVertices = vertexDataSeqs.map(_.size).sum
    val data = new Array[Float](nComponents * nVertices)
    var i = 0
    for (
      vertexData <- vertexDataSeqs;
      (pos, col) <- vertexData
    ) {
      for (j <- 0 until nCompPos) {
        data(i * nComponents + j) = pos(j)
      }
      for (j <- 0 until nCompCol) {
        assert(col != null)
        data(i * nComponents + j + nCompPos) = col(j)
      }
      i += 1
    }

    createVBO(data, dynamic)
  }
}


private[graphics] object Material {
  private[materials] var _drawCalls = 0
  private[materials] var _modelviewUploads = 0

  def resetDrawCalls(): Unit = _drawCalls = 0
  def resetModelviewUploads(): Unit = _modelviewUploads = 0
  def drawCalls = _drawCalls
  def modelviewUploads = _modelviewUploads
}

