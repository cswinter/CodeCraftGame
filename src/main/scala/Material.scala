import javax.media.opengl.GL._
import javax.media.opengl.GL2ES2._

import scala.io.Source

import javax.media.opengl._



/**
 * Vertex shader: code run on GPU to transform vertex positions
 * Fragment shader: code run on GPU to determine pixel colours
 * Program: can be used to store and then reference a vertex + fragment shader on the GPU
 * Vertex Buffer Object: unstructured vertex data
 * (Vertex) Attribute: input parameter to a shader
 * Vertex Attribute Object: maps data from VBO to one or more attributes
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


  /********************
   * PUBLIC INTERFACE *
   ********************/

  def beforeDraw(): Unit = {
    glUseProgram(programID)
  }

  def draw(vbo: Int): Unit = {
    // bind vao, and vbo and enable attributes
    //glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glEnableVertexAttribArray(attributeVP)

    // actual drawing call
    glDrawArrays(GL_TRIANGLES, 0, 3)
  }

  def afterDraw(): Unit = {
    // disable attributes
    glDisableVertexAttribArray(attributeVP)

    // check logs for errors
    checkProgramInfoLog(programID)
    checkShaderInfoLog(fragmentShaderID)
    checkShaderInfoLog(vertexShaderID)
  }


  /*******************
   * PRIVATE METHODS *
   *******************/

  def bindAttributes(vbo: Int): Unit = {
    glVertexAttribPointer(attributeVP, 3, GL_FLOAT, false, 12, 0)
  }

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
