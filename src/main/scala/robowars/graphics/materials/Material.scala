package robowars.graphics.materials

import javax.media.opengl.GL._
import javax.media.opengl.GL2ES2._
import javax.media.opengl._

import com.jogamp.common.nio.Buffers
import robowars.graphics.matrices.Matrix4x4
import robowars.graphics.model.{VBO, Vertex, VertexManifest}

import scala.io.Source



/**
 * Vertex shader: code run on GPU to transform vertex positions
 * Fragment shader: code run on GPU to determine pixel colours
 * Program: can be used to store and then reference a vertex + fragment shader on the GPU
 * Vertex Buffer Object: unstructured vertex data
 * (Vertex) Attribute: input parameter to a shader
 * Vertex Attribute Object: maps data from robowars.graphics.model.VBO to one or more attributes
 */
class Material[TPosition <: Vertex, TColor <: Vertex](
  val gl: GL4,
  vsPath: String,
  fsPath: String,
  attributeNamePos: String,
  attributeNameCol: Option[String],
  enableCaps: Int*)
(implicit val posVM: VertexManifest[TPosition], val colVM: VertexManifest[TColor]) {

  /******************
   * INITIALISATION *
   ******************/

  import gl._

  // compile shaders and attach to program
  protected val programID = glCreateProgram()
  protected val vertexShaderID = compileShader(vsPath, GL_VERTEX_SHADER, programID)
  protected val fragmentShaderID = compileShader(fsPath, GL_FRAGMENT_SHADER, programID)
  glLinkProgram(programID)
  checkProgramInfoLog(programID)

  val uniformProjection = glGetUniformLocation(programID, "projection")
  val uniformModelview = glGetUniformLocation(programID, "modelview")

  val attributePos = glGetAttribLocation(programID, attributeNamePos)
  val attributeCol = attributeNameCol.map(glGetAttribLocation(programID, _))


  /********************
   * PUBLIC INTERFACE *
   ********************/

  def beforeDraw(projection: Matrix4x4): Unit = {
    glUseProgram(programID)

    glUniformMatrix4fv(
      uniformProjection,
      1 /* only setting 1 matrix */,
      true /* transpose? */,
      projection.data,
      0 /* offset */)

    enableCaps.foreach(glEnable)
  }

  def draw(vbo: VBO, modelview: Matrix4x4): Unit = {
    // upload modelview
    glUniformMatrix4fv(uniformModelview, 1, true, modelview.data, 0)

    // bind vbo and enable attributes
    gl.glBindVertexArray(vbo.vao)
    glBindBuffer(GL_ARRAY_BUFFER, vbo.id)

    glEnableVertexAttribArray(attributePos)
    attributeCol.foreach(glEnableVertexAttribArray)

    // actual drawing call
    glDrawArrays(GL_TRIANGLES, 0, vbo.size)
  }

  def afterDraw(): Unit = {
    enableCaps.foreach(glDisable)

    // disable attributes
    glDisableVertexAttribArray(attributePos)
    attributeCol.foreach(glDisableVertexAttribArray)

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
  def createVBO(vertexData: Seq[(TPosition, TColor)]): VBO = {
    val nCompPos = posVM.nComponents
    val nCompCol = colVM.nComponents
    val nComponents = nCompPos + nCompCol
    val data = new Array[Float](nComponents * vertexData.size)
    for (((pos, col), i) <- vertexData.zipWithIndex) {
      for (j <- 0 until nCompPos) {
        data(i * nComponents + j) = pos(j)
      }
      for (j <- 0 until nCompCol) {
        data(i * nComponents + j + nCompPos) = col(j)
      }
    }


    // create vbo handle
    val vboRef = new Array[Int](1)
    glGenBuffers(1, vboRef, 0)
    val vboHandle = vboRef(0)

    val vaoRef = new Array[Int](1)
    glGenVertexArrays(1, vaoRef, 0)
    val vao = vaoRef(0)

    glBindVertexArray(vao)


    // store data to GPU
    glBindBuffer(GL_ARRAY_BUFFER, vboHandle)
    val numBytes = data.length * 4
    val verticesBuffer = Buffers.newDirectFloatBuffer(data)
    glBufferData(GL_ARRAY_BUFFER, numBytes, verticesBuffer, GL_STATIC_DRAW)

    // bind shader attributes (input parameters)
    glVertexAttribPointer(attributePos, nCompPos, GL_FLOAT, false, 4 * nComponents, 0)
    attributeCol.foreach(glVertexAttribPointer(_, nCompCol, GL_FLOAT, false, 4 * nComponents, 4 * nCompPos))

    glBindBuffer(GL_ARRAY_BUFFER, 0)

    VBO(vboHandle, vertexData.length, vao)
  }


  /*******************
   * PRIVATE METHODS *
   *******************/


  /**
   * Compile a shader and attach to a program.
   * @param filepath The source code for the shader.
   * @param shaderType The type of shader (`GL2ES2.GL_VERTEX_SHADER` or `GL2ES2.GL_FRAGMENT_SHADER`)
   * @param programID The handle to the program.
   * @return
   */
  protected def compileShader(filepath: String, shaderType: Int, programID: Int): Int = {
    // Create GPU shader handles
    // OpenGL returns an index id to be stored for future reference.
    val shaderHandle = glCreateShader(shaderType)

    // bind shader to program
    glAttachShader(programID, shaderHandle)


    // Load shader source code and compile into a program
    val lines = Array(Source.fromFile(filepath).mkString)
    val lengths = lines.map(_.length)
    glShaderSource(shaderHandle, lines.length, lines, lengths, 0)
    glCompileShader(shaderHandle)


    // Check compile status.
    val compiled = new Array[Int](1)
    glGetShaderiv(shaderHandle, GL_COMPILE_STATUS, compiled, 0)
    if (compiled(0) != 0) {
      println("Horray! shader compiled")
    } else {
      println("Error compiling shader:")
      checkShaderInfoLog(shaderHandle)
    }

    shaderHandle
  }


  /**
   * Print out errors from the program info log, if any.
   */
  protected def checkProgramInfoLog(programID: Int): Unit = {
    // obtain log message byte count
    val logLength = new Array[Int](1)
    glGetProgramiv(programID, GL_INFO_LOG_LENGTH, logLength, 0)

    if (logLength(0) > 1) {
      val log = new Array[Byte](logLength(0))
      glGetProgramInfoLog(programID, logLength(0), null, 0, log, 0)
      println(s"Program Error:\n${new String(log)}")
    }
  }


  /**
   * Print out errors from the shader info log, if any.
   */
  protected def checkShaderInfoLog(shaderID: Int): Unit = {
    val logLength = new Array[Int](1)
    glGetShaderiv(shaderID, GL_INFO_LOG_LENGTH, logLength, 0)

    if (logLength(0) > 1) {
      val log = new Array[Byte](logLength(0))
      glGetShaderInfoLog(shaderID, logLength(0), null, 0, log, 0)

      println(new String(log))
    }
  }
}
