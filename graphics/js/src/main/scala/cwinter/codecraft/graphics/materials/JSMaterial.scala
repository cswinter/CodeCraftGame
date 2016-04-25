package cwinter.codecraft.graphics.materials

import cwinter.codecraft.graphics.model.{JSVBO, VBO}
import cwinter.codecraft.util.maths.matrices.Matrix4x4
import cwinter.codecraft.util.maths.{Vertex, VertexManifest}
import org.scalajs.dom.raw.{WebGLProgram, WebGLRenderingContext => GL, WebGLShader}

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
private[graphics] class JSMaterial[TPosition <: Vertex, TColor <: Vertex, TParams](
  val gl: GL,
  vsSource: String,
  fsSource: String,
  attributeNamePos: String,
  attributeNameCol: Option[String],
  enableCaps: Int*
)(implicit
  val posVM: VertexManifest[TPosition],
  val colVM: VertexManifest[TColor]
) extends Material[TPosition, TColor, TParams]{

  val nCompPos = posVM.nComponents
  val nCompCol = colVM.nComponents
  val nComponents = nCompPos + nCompCol


  /*#################
   # INITIALISATION #
   #################*/

  // compile shaders and attach to program
  protected val programID = gl.createProgram()
  protected val vertexShaderID = compileShader(vsSource, GL.VERTEX_SHADER, programID)
  protected val fragmentShaderID = compileShader(fsSource, GL.FRAGMENT_SHADER, programID)
  gl.linkProgram(programID)

  val uniformProjection = gl.getUniformLocation(programID, "projection")
  val uniformModelview = gl.getUniformLocation(programID, "modelview")

  val attributePos = gl.getAttribLocation(programID, attributeNamePos)
  val attributeCol = attributeNameCol.map(gl.getAttribLocation(programID, _))

  private var lastModelview: Matrix4x4 = null


  implicit def arrayFloatToFloat32Array(seq: Array[Float]): Float32Array = {
    import js.JSConverters._
    new Float32Array(seq.toJSArray)
  }

  implicit def VBOToJSVBO(vbo: VBO): JSVBO = {
    assert(vbo.isInstanceOf[JSVBO], s"Expected vbo of type JSVBO. Actual: ${vbo.getClass.getName}")
    vbo.asInstanceOf[JSVBO]
  }

  /*###################
   # PUBLIC INTERFACE #
   ###################*/

  def beforeDraw(projection: Matrix4x4): Unit = {
    lastModelview = null
    gl.useProgram(programID)

    gl.uniformMatrix4fv(
      uniformProjection,
      transpose=false,
      projection.data)

    enableCaps.foreach(gl.enable)
  }

  def draw(vbo: VBO, modelview: Matrix4x4): Unit = {
    Material._drawCalls += 1

    // upload modelview
    if (lastModelview ne modelview) {
      gl.uniformMatrix4fv(uniformModelview, transpose = false, modelview.data)
      lastModelview = modelview
      Material._modelviewUploads += 1
    }

    // bind vbo and enable attributes
    gl.bindBuffer(GL.ARRAY_BUFFER, vbo.id)

    // bind shader attributes (input parameters)
    gl.enableVertexAttribArray(attributePos)
    attributeCol.foreach(gl.enableVertexAttribArray)
    gl.vertexAttribPointer(attributePos, nCompPos, GL.FLOAT, normalized=false, 4 * nComponents, 0)
    attributeCol.foreach(gl.vertexAttribPointer(_, nCompCol, GL.FLOAT, normalized=false, 4 * nComponents, 4 * nCompPos))

    // actual drawing call
    gl.drawArrays(GL.TRIANGLES, 0, vbo.size)
  }

  def afterDraw(): Unit = {
    enableCaps.foreach(gl.disable)

    // disable attributes
    gl.disableVertexAttribArray(attributePos)
    attributeCol.foreach(gl.disableVertexAttribArray)
  }


  /**
   * Allocates a VBO handle, loads vertex data into GPU and defines attribute pointers.
   * @param data The data for the VBO.
   * @return Returns a `robowars.graphics.model.VBO` class which give the handle and number of data of the vbo.
   */
  def createVBO(data: Array[Float], dynamic: Boolean): VBO = {
    // create vbo handle
    val vboHandle = gl.createBuffer()

    // store data to GPU
    gl.bindBuffer(GL.ARRAY_BUFFER, vboHandle)
    gl.bufferData(GL.ARRAY_BUFFER, data, if (dynamic) GL.DYNAMIC_DRAW else GL.STATIC_DRAW)

    VBO._count += 1
    JSVBO(vboHandle, data.length / nComponents)
  }


  /*##################
   # PRIVATE METHODS #
   ##################*/


  /**
   * Compile a shader and attach to a program.
   * @param shaderSource The source code for the shader.
   * @param shaderType The type of shader (`GL2ES2.GL.VERTEX_SHADER` or `GL2ES2.GL.FRAGMENT_SHADER`)
   * @param programID The handle to the program.
   * @return
   */
  protected def compileShader(
    shaderSource: String,
    shaderType: Int,
    programID: WebGLProgram
  ): WebGLShader = {

    // Create GPU shader handles
    // OpenGL returns an index id to be stored for future reference.
    val shaderHandle = gl.createShader(shaderType)

    // bind shader to program
    gl.attachShader(programID, shaderHandle)


    // Load shader source code and compile into a program
    gl.shaderSource(shaderHandle, shaderSource)
    gl.compileShader(shaderHandle)

    val compileStatus = gl.getShaderParameter(shaderHandle, GL.COMPILE_STATUS)
    if (!compileStatus.asInstanceOf[Boolean]) {
      throw new Exception(gl.getShaderInfoLog(shaderHandle))
    }

    shaderHandle
  }


  override def dispose(): Unit = {
    gl.deleteProgram(programID)
  }
}

