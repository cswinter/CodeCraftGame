package graphics

import javax.media.opengl.GL._
import javax.media.opengl.GL2ES2._
import javax.media.opengl._

import com.jogamp.common.nio.Buffers

import scala.io.Source



/**
 * Vertex shader: code run on GPU to transform vertex positions
 * Fragment shader: code run on GPU to determine pixel colours
 * Program: can be used to store and then reference a vertex + fragment shader on the GPU
 * Vertex Buffer Object: unstructured vertex data
 * (Vertex) Attribute: input parameter to a shader
 * Vertex Attribute Object: maps data from graphics.VBO to one or more attributes
 */
class Material(val gl: GL4, vsPath: String, fsPath: String) {
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

  // define vertex attribute object (maps vbo data to shader variables)
  protected val attributeVP = glGetAttribLocation(programID, "vp")
  protected val uniformProjection = glGetUniformLocation(programID, "projection")


  /********************
   * PUBLIC INTERFACE *
   ********************/

  def beforeDraw(projection: Array[Float]): Unit = {

    glUniformMatrix4fv(
      uniformProjection,
      1 /* only setting 1 matrix */,
      false /* transpose? */,
      projection,
      0 /* offset */)

    glUseProgram(programID)

  }

  def draw(vbo: VBO): Unit = {
    // bind vbo and enable attributes
    gl.glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vbo.id)
    glEnableVertexAttribArray(attributeVP)


    // actual drawing call
    glDrawArrays(GL_TRIANGLES, 0, 6)
  }

  def afterDraw(): Unit = {
    // disable attributes
    glDisableVertexAttribArray(attributeVP)

    // check logs for errors
    checkProgramInfoLog(programID)
    checkShaderInfoLog(fragmentShaderID)
    checkShaderInfoLog(vertexShaderID)
  }

  var vao: Int = 0
  /**
   * Allocates a graphics.VBO handle, loads vertex data into GPU and defines attribute pointers.
   * @param vertexData The data for the graphics.VBO.
   * @return Returns a `graphics.VBO` class which give the handle and number of data of the vbo.
   */
  def createVBO(vertexData: Array[Float]): VBO = {
    val DATA_LENGTH = 3 // TODO: make dynamic
    // create vbo handle
    val vboRef = new Array[Int](1)
    glGenBuffers(1, vboRef, 0)
    val vboHandle = vboRef(0)
    val vbo = VBO(vboHandle, vertexData.length / DATA_LENGTH)

    val vaoRef = new Array[Int](1)
    glGenVertexArrays(1, vaoRef, 0)
    vao = vaoRef(0)

    glBindVertexArray(vao)

    // store data to GPU
    glBindBuffer(GL_ARRAY_BUFFER, vboHandle)
    val numBytes = vertexData.length * 4
    val verticesBuffer = Buffers.newDirectFloatBuffer(vertexData)
    glBufferData(GL_ARRAY_BUFFER, numBytes, verticesBuffer, GL_STATIC_DRAW)


    // bind shader attributes (input parameters)
    glVertexAttribPointer(attributeVP, DATA_LENGTH, GL_FLOAT, false, 4 * DATA_LENGTH, 0)
    glBindBuffer(GL_ARRAY_BUFFER, 0)

    vbo
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
  private def compileShader(filepath: String, shaderType: Int, programID: Int): Int = {
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
  private def checkProgramInfoLog(programID: Int): Unit = {
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
  private def checkShaderInfoLog(shaderID: Int): Unit = {
    val logLength = new Array[Int](1)
    glGetShaderiv(shaderID, GL_INFO_LOG_LENGTH, logLength, 0)

    if (logLength(0) > 1) {
      val log = new Array[Byte](logLength(0))
      glGetShaderInfoLog(shaderID, logLength(0), null, 0, log, 0)

      println(new String(log))
    }
  }
}
