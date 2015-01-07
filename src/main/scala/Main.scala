import java.awt.Frame
import java.awt.event.{WindowAdapter, WindowEvent}
import javax.media.opengl._
import javax.media.opengl.awt.GLCanvas

import com.jogamp.opengl.util.FPSAnimator
import graphics._


object Main extends GLEventListener {
  var gl: GL4 = null
  var material: Material = null
  var triangle: Model = null
  val Debug = false

  override def display(drawable: GLAutoDrawable): Unit = {
    update()
    render(drawable)
  }

  private def render(drawable: GLAutoDrawable): Unit = {
    var gl = drawable.getGL.getGL4
    if (Debug) {
      gl = drawable.setGL(new DebugGL4(gl)).getGL4
    }

    // set background color
    gl.glClearColor(0, 0, 0, 0.0f)
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT)

    material.beforeDraw()

    triangle.draw()

    material.afterDraw()
  }

  private def update(): Unit = {
  }

  def dispose(arg0: GLAutoDrawable): Unit = {
    // stub
  }


  def init(drawable: GLAutoDrawable): Unit = {
    implicit var gl: GL4 = drawable.getGL.getGL4
    if (Debug) {
      gl = drawable.setGL(new DebugGL4(gl)).getGL4
    }

    println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities)
    println("INIT GL IS: " + gl.getClass.getName)
    println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR))
    println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER))
    println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION))

    material = new Material(gl, "src/main/shaders/vs_basic.glsl", "src/main/shaders/fs_basic.glsl")

    var modelBuilder: Model = new ModelBuilder(
      material,
      Array[Float](
         0,  1, 1,
         1,  0, 1,
         0, -1, 1
      )
    )

    modelBuilder += new ModelBuilder(
      material,
      Array[Float](
         0,  1, 1,
         0, -1, 1,
        -1,  0, 1
      )
    )

    triangle = modelBuilder.init()
  }

  def reshape(arg0: GLAutoDrawable, arg1: Int, arg2: Int, arg3: Int, arg4: Int): Unit = {

  }


  def main(args: Array[String]): Unit = {
    GLProfile.initSingleton()
    val glp = GLProfile.getDefault
    val caps = new GLCapabilities(glp)
    val canvas = new GLCanvas(caps)
    canvas.addGLEventListener(this)

    val frame = new Frame("AWT Window Test")
    frame.setSize(300, 300)
    frame.add(canvas)
    frame.setVisible(true)

    // by default, an AWT Frame doesn't do anything when you click
    // the close button; this bit of code will terminate the program when
    // the window is asked to close
    frame.addWindowListener(new WindowAdapter {
      override def windowClosing(e: WindowEvent) {
        System.exit(0)
      }
    })

    val animator = new FPSAnimator(canvas, 60)
    animator.start()


  }
}
