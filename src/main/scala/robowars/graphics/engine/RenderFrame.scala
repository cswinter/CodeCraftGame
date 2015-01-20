package robowars.graphics.engine

import java.awt.TextField

import javax.media.opengl._
import javax.media.opengl.GL._

import org.joda.time.DateTime

import robowars.simulation.GameWorldSimulator



object RenderFrame extends GLEventListener {
  val Debug = false

  var gl: GL4 = null
  implicit var fbo: FramebufferObject = null
  implicit var renderStack: RenderStack = null
  var visualizer: Visualizer = null
  var camera = new Camera2D

  var cullFaceToggle = false
  val FrametimeSamples = 100
  var frameTimes = scala.collection.mutable.Queue.fill(FrametimeSamples - 1)(new DateTime().getMillis)
  var textField: TextField = null


  override def display(drawable: GLAutoDrawable): Unit = {
    update()
    render(drawable)
  }

  private def render(drawable: GLAutoDrawable): Unit = {
    val gl = getGL(drawable)
    import gl._

    if (cullFaceToggle) glEnable(GL_CULL_FACE)
    else glDisable(GL_CULL_FACE)
    cullFaceToggle = !cullFaceToggle


    // draw to texture
    glBindFramebuffer(GL_FRAMEBUFFER, fbo.fbo)
    glViewport(0, 0, camera.screenWidth, camera.screenHeight)

    glClearColor(0.1f, 0, 0.1f, 0.0f)
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

    glDisable(GL_CULL_FACE)

    val models = visualizer.computeModels(GameWorldSimulator.worldState)

    for (material <- renderStack.materials) {
      material.beforeDraw(camera.projection)

      for (model <- models if model.hasMaterial(material))
        model.project(material).draw()

      material.afterDraw()
    }

    // draw to screen
    renderStack.postDraw(camera)


    // update fps
    val now = new DateTime().getMillis
    frameTimes.enqueue(now)
    val then = frameTimes.dequeue()
    val fps = FrametimeSamples * 1000 / (now - then)
    textField.setText(s"FPS: $fps")
  }

  private def update(): Unit = {
    GameWorldSimulator.worldState
  }

  def dispose(arg0: GLAutoDrawable): Unit = {
    // stub
  }


  def init(drawable: GLAutoDrawable): Unit = {
    implicit val gl = getGL(drawable)
    import gl._

    println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities)
    println("INIT GL IS: " + gl.getClass.getName)
    println("GL_VENDOR: " + glGetString(GL.GL_VENDOR))
    println("GL_RENDERER: " + glGetString(GL.GL_RENDERER))
    println("GL_VERSION: " + glGetString(GL.GL_VERSION))

    // vsync to prevent screen tearing.
    // seems to work with Ubuntu + i3, but might not be portable
    setSwapInterval(1)

    fbo = new FramebufferObject
    renderStack = new RenderStack
    visualizer = new Visualizer
  }

  def reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int): Unit = {
    camera.screenDims = (width, height)

    fbo.resize(width, height)(gl)

    println(s"reshape($x, $y, $width, $height)")
  }

  def getGL(drawable: GLAutoDrawable): GL4 =
    if (Debug) {
      drawable.setGL(new DebugGL4(drawable.getGL.getGL4)).getGL4
    } else {
      drawable.getGL.getGL4
    }
}
