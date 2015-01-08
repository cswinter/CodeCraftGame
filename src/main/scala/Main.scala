import java.awt.BorderLayout
import javax.media.opengl._
import javax.media.opengl.glu.GLU
import javax.media.opengl.awt.{GLJPanel, GLCanvas}

import com.jogamp.opengl.util.FPSAnimator
import graphics._

import scala.swing._
import scala.swing.event._


object MyGLEventListener extends GLEventListener {
  var gl: GL4 = null
  var material: Material = null
  var triangle: Model = null
  val Debug = false
  var projection: Array[Float] = null


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
    projection = Array[Float](
      1.0f / width, 0.0f, 0.0f, 0.0f,
      0.0f, 1.0f / height, 0.0f, 0.0f,
      0.0f, 0.0f, 1.0f, 0.0f,
      0.0f, 0.0f, 0.0f, 1.0f
    )
    println(s"reshape($x, $y, $width, $height)")
  }

  def getGL(drawable: GLAutoDrawable): GL4 =
    if (Debug) {
      drawable.setGL(new DebugGL4(drawable.getGL.getGL4)).getGL4
    } else {
      drawable.getGL.getGL4
    }
}


object HelloWorld extends SwingApplication {

  def startup(args: Array[String]): Unit = {

    GLProfile.initSingleton()
    val glp = GLProfile.getDefault
    val caps = new GLCapabilities(glp)
    val canvas = new GLCanvas(caps)
    canvas.addGLEventListener(MyGLEventListener)

    val frame = new MainFrame()
    frame.resizable = true
    frame.peer.setSize(1920, 1080)
    frame.peer.add(canvas, BorderLayout.CENTER)
    frame.peer.setVisible(true)

    val animator = new FPSAnimator(canvas, 60)
    animator.start()
  }
}

