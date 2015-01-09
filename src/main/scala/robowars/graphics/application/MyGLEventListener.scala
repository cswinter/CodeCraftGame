package robowars.graphics.application

import javax.media.opengl._

import robowars.graphics.matrices._
import robowars.graphics.model._


object MyGLEventListener extends GLEventListener {
  var gl: GL4 = null
  var material: Material = null
  var triangle: DrawableModel = null
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

  var time = 0.0f
  private def update(): Unit = {
    time += 0.05f
    val t = time + 0.5 * math.sin(1.41 * time).toFloat
    val t2 = time + 1.5 * math.cos(1.73 * time).toFloat
    val translation = new TranslationXYMatrix4x4(300 * math.sin(t2).toFloat, 300 * math.cos(t).toFloat)
    val rotation = new RotationZMatrix4x4(3 * t2)
    triangle.setModelview(rotation * translation)
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

    // vsync to prevent screen tearing.
    // seems to work with Ubuntu + i3, but might not be portable
    setSwapInterval(1)

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
