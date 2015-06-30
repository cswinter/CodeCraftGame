package cwinter.codecraft.graphics.materials

import org.scalajs.dom.raw.{WebGLRenderingContext => GL, WebGLShader, WebGLProgram}
import org.scalajs.dom.document
import org.scalajs.dom.ext._

import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.graphics.model.VBO
import cwinter.codecraft.util.maths.{VertexManifest, Vertex}

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.typedarray.Float32Array


/**
 * Vertex shader: code run on GPU to transform vertex positions
 * Fragment shader: code run on GPU to determine pixel colours
 * Program: can be used to store and then reference a vertex + fragment shader on the GPU
 * Vertex Buffer Object: unstructured vertex data
 * (Vertex) Attribute: input parameter to a shader
 * Vertex Attribute Object: maps data from robowars.graphics.model.VBO to one or more attributes
 */
class Material[TPosition <: Vertex, TColor <: Vertex, TParams](
  val gl: GL,
  vsID: String,
  fsID: String,
  attributeNamePos: String,
  attributeNameCol: Option[String],
  enableCaps: Int*)
    (implicit val posVM: VertexManifest[TPosition], val colVM: VertexManifest[TColor]) {

  /******************
    * INITIALISATION *
    ******************/

  var params: TParams = _

  // compile shaders and attach to program
  protected val programID = gl.createProgram()
  protected val vertexShaderID = compileShader(vsID, GL.VERTEX_SHADER, programID)
  protected val fragmentShaderID = compileShader(fsID, GL.FRAGMENT_SHADER, programID)
  gl.linkProgram(programID)
  checkProgramInfoLog(programID)

  val uniformProjection = gl.getUniformLocation(programID, "projection")
  val uniformModelview = gl.getUniformLocation(programID, "modelview")

  val attributePos = gl.getAttribLocation(programID, attributeNamePos)
  val attributeCol = attributeNameCol.map(gl.getAttribLocation(programID, _))

  implicit def arrayFloatToFloat32Array(seq: Array[Float]): Float32Array =
    new Float32Array(js.Array[Float](seq: _*))

  /********************
    * PUBLIC INTERFACE *
    ********************/

  def beforeDraw(projection: Matrix4x4): Unit = {
    gl.useProgram(programID)

    gl.uniformMatrix4fv(
      uniformProjection,
      transpose=false,
      new Float32Array(js.Array[Float](projection.data: _*)))

    enableCaps.foreach(gl.enable)
  }

  def draw(vbo: VBO, modelview: Matrix4x4): Unit = {
    Material._drawCalls += 1

    // upload modelview
    gl.uniformMatrix4fv(uniformModelview, transpose=false, modelview.data)

    // bind vbo and enable attributes
    // gl.bindVertexArray(vbo.vao) // TODO: ???
    gl.bindBuffer(GL.ARRAY_BUFFER, vbo.id)

    gl.enableVertexAttribArray(attributePos)
    attributeCol.foreach(gl.enableVertexAttribArray)

    // actual drawing call
    gl.drawArrays(GL.TRIANGLES, 0, vbo.size)
  }

  def afterDraw(): Unit = {
    enableCaps.foreach(gl.disable)

    // disable attributes
    gl.disableVertexAttribArray(attributePos)
    attributeCol.foreach(gl.disableVertexAttribArray)

    // check logs for errors
    checkProgramInfoLog(programID)
    checkShaderInfoLog(fragmentShaderID)
    checkShaderInfoLog(vertexShaderID)
  }


  /**
   * Allocates a VBO handle, loads vertex data into GPU and defines attribute pointers.
   * @param vertexData The data for the VBO.
   * @return Returns a `robowars.graphics.model.VBO` class which give the handle and number of data of the vbo.
   */
  def createVBO(vertexData: Seq[(TPosition, TColor)], dynamic: Boolean = false): VBO = {
    val nCompPos = posVM.nComponents
    val nCompCol = colVM.nComponents
    val nComponents = nCompPos + nCompCol
    val data = new Array[Float](nComponents * vertexData.size)
    for (((pos, col), i) <- vertexData.zipWithIndex) {
      for (j <- 0 until nCompPos) {
        data(i * nComponents + j) = pos(j)
      }
      for (j <- 0 until nCompCol) {
        assert(col != null)
        data(i * nComponents + j + nCompPos) = col(j)
      }
    }


    // create vbo handle
    val vboHandle = gl.createBuffer()

    //val vaoRef = new Array[Int](1)
    //glGenVertexArrays(1, vaoRef, 0)
    //val vao = vaoRef(0) // TODO: ???
    val vao = 0

    // glBindVertexArray(vao) TODO: ???


    // store data to GPU
    gl.bindBuffer(GL.ARRAY_BUFFER, vboHandle)
    gl.bufferData(GL.ARRAY_BUFFER, data, if (dynamic) GL.DYNAMIC_DRAW else GL.STATIC_DRAW)

    // bind shader attributes (input parameters)
    gl.vertexAttribPointer(attributePos, nCompPos, GL.FLOAT, normalized=false, 4 * nComponents, 0)
    attributeCol.foreach(gl.vertexAttribPointer(_, nCompCol, GL.FLOAT, normalized=false, 4 * nComponents, 4 * nCompPos))

    gl.bindBuffer(GL.ARRAY_BUFFER, null)

    VBO._count += 1
    VBO(vboHandle, vertexData.length, vao)
  }


  /*******************
    * PRIVATE METHODS *
    *******************/


  /**
   * Compile a shader and attach to a program.
   * @param shaderScriptID The source code for the shader.
   * @param shaderType The type of shader (`GL2ES2.GL.VERTEX_SHADER` or `GL2ES2.GL.FRAGMENT_SHADER`)
   * @param programID The handle to the program.
   * @return
   */
  protected def compileShader(
    shaderScriptID: String,
    shaderType: Int,
    programID: WebGLProgram
  ): WebGLShader = {

    // Create GPU shader handles
    // OpenGL returns an index id to be stored for future reference.
    val shaderHandle = gl.createShader(shaderType)

    // bind shader to program
    gl.attachShader(programID, shaderHandle)


    // Load shader source code and compile into a program

    val shaderScript = document.getElementById(shaderScriptID)
    val lines = for (
      k <- shaderScript.childNodes
      if k.nodeType == 3
    ) yield k.textContent
    val shaderSource = lines.mkString
    gl.shaderSource(shaderHandle, shaderSource)
    gl.compileShader(shaderHandle)

    val compileStatus = gl.getShaderParameter(shaderHandle, GL.COMPILE_STATUS)
    println("Compile status: " + compileStatus)
    if (!compileStatus.asInstanceOf[Boolean]) {
      throw new Exception(gl.getShaderInfoLog(shaderHandle))
    }
    /**
    // Check compile status.
    val compiled = new Array[Int](1)
    gl.getShaderiv(shaderHandle, GL.COMPILE_STATUS, compiled, 0)
    if (compiled(0) == 0) {
      println("Error compiling shader:")
      checkShaderInfoLog(shaderHandle)
    }*/ // TODO: error checking

    shaderHandle
  }


  /**
   * Print out errors from the program info log, if any.
   */
  protected def checkProgramInfoLog(programID: WebGLProgram): Unit = {
    /*
    // obtain log message byte count
    val logLength = new Array[Int](1)
    glGetProgramiv(programID, GL.INFO_LOG_LENGTH, logLength, 0)

    if (logLength(0) > 1) {
      val log = new Array[Byte](logLength(0))
      glGetProgramInfoLog(programID, logLength(0), null, 0, log, 0)
      println(s"Program Error:\n${new String(log)}")
    }
    */ // TODO: implement
  }


  /**
   * Print out errors from the shader info log, if any.
   */
  protected def checkShaderInfoLog(shaderID: WebGLShader): Unit = {
    /**
    val logLength = new Array[Int](1)
    glGetShaderiv(shaderID, GL.INFO_LOG_LENGTH, logLength, 0)

    if (logLength(0) > 1) {
      val log = new Array[Byte](logLength(0))
      glGetShaderInfoLog(shaderID, logLength(0), null, 0, log, 0)

      println(new String(log))
    }*/ //TODO: implement
  }
}


object Material {
  private var _drawCalls = 0

  def resetDrawCalls(): Unit = _drawCalls = 0
  def drawCalls = _drawCalls
}

