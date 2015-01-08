package robowars.graphics.application

import javax.media.opengl._

import robowars.graphics.matrices.{OrthographicProjectionMatrix4x4, Matrix4x4}
import robowars.graphics.model.{Material, Model, ModelBuilder}


object MyGLEventListener extends GLEventListener {
  var gl: GL4 = null
  var material: Material = null
  var triangle: Model = null
  val Debug = false
  var projection: Matrix4x4 = null


  override def display(drawable: GLAutoDrawable): Unit = {
    update()
    render(drawable)
  }

  private def render(drawable: GLAutoDrawable): Unit = {
    val gl = getGL(drawable)
    import gl._


    //val glu = GLU.createGLU()
    //glu.gluPerspective(45, /* width height ratio */0, 1, 1000)

    // set background color
    glClearColor(0, 0, 1, 0.0f)
    glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT)


    material.beforeDraw(projection)

    triangle.draw()

    material.afterDraw()
  }

  private def update(): Unit = {
  }

  def dispose(arg0: GLAutoDrawable): Unit = {
    // stub
  }


  def init(drawable: GLAutoDrawable): Unit = {
    val gl = getGL(drawable)
    import gl._

    println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities)
    println("INIT GL IS: " + gl.getClass.getName)
    println("GL_VENDOR: " + glGetString(GL.GL_VENDOR))
    println("GL_RENDERER: " + glGetString(GL.GL_RENDERER))
    println("GL_VERSION: " + glGetString(GL.GL_VERSION))

    material = new Material(gl, "src/main/shaders/vs_basic.glsl", "src/main/shaders/fs_basic.glsl")

    var modelBuilder: Model = new ModelBuilder(
      material,
      Array[Float](
          0,  100, 1,
          100,  0, 1,
          0, -100, 1
      )
    )

    modelBuilder += new ModelBuilder(
      material,
      Array[Float](
           0,  100, 1,
           0, -100, 1,
        -100,  0, 1
      )
    )

    triangle = modelBuilder.init()
  }

  def reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int): Unit = {
    projection = new OrthographicProjectionMatrix4x4(width, height)
    println(s"reshape($x, $y, $width, $height)")
  }

  def getGL(drawable: GLAutoDrawable): GL4 =
    if (Debug) {
      drawable.setGL(new DebugGL4(drawable.getGL.getGL4)).getGL4
    } else {
      drawable.getGL.getGL4
    }
}
