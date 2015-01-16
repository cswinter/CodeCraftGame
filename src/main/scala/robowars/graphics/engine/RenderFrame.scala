package robowars.graphics.engine

import java.awt.BorderLayout
import java.awt.event.{KeyEvent, KeyListener}
import javax.media.opengl._
import javax.media.opengl.GL._
import javax.media.opengl.awt.GLCanvas

import com.jogamp.opengl.util.FPSAnimator

import robowars.graphics.model._
import robowars.simulation.GameWorldSimulator

import scala.swing.MainFrame


object RenderFrame extends MainFrame with GLEventListener {
  val Debug = false


  // Setup code
  GLProfile.initSingleton()
  val glp = GLProfile.getDefault
  val caps = new GLCapabilities(glp)
  val canvas = new GLCanvas(caps)
  canvas.addGLEventListener(this)
  resizable = true
  peer.setSize(1920, 1080)
  peer.add(canvas, BorderLayout.CENTER)
  peer.setVisible(true)
  new FPSAnimator(canvas, 60).start()
  canvas.transferFocus()


  var gl: GL4 = null
  var simpleMaterial: SimpleMaterial = null
  var materialXYRGB: MaterialXYZRGB = null
  var textureToScreen: RenderToScreen = null
  var bloomShader: BloomShader = null
  var triangle: DrawableModel = null
  var quad: DrawableModel = null
  var camera = new Camera2D()
  var fbo: FramebufferObject = null

  var cullFaceToggle = false

  val visualizer = new Visualizer()


  canvas.addKeyListener(new KeyListener {
    val moveSpeed = 100
    val zoomSpeed = 0.2f

    override def keyTyped(keyEvent: KeyEvent): Unit = println(s"keyTyped($keyEvent)")

    override def keyPressed(keyEvent: KeyEvent): Unit = keyEvent.getKeyCode match {
      case 37 /* LEFT */ => camera.x -= moveSpeed * camera.zoomFactor
      case 39 /* RIGHT */ => camera.x += moveSpeed * camera.zoomFactor
      case 38 /* UP */ => camera.y += moveSpeed * camera.zoomFactor
      case 40 /* DOWN */ => camera.y -= moveSpeed * camera.zoomFactor
      case 33 /* PAGE UP */ => camera.zoom -= zoomSpeed
      case 34 /* PAGE DOWN */ => camera.zoom += zoomSpeed
      case _ =>
    } //println(s"keyPressed($keyEvent)")

    override def keyReleased(keyEvent: KeyEvent): Unit = println(s"keyReleased($keyEvent)")
  })

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

    for (material <- Seq(simpleMaterial, materialXYRGB, bloomShader)) {
      material.beforeDraw(camera.projection)

      for (model <- models if model.hasMaterial(material))
        model.project(material).draw()

      material.afterDraw()
    }


    // draw texture to screen
    glViewport(0, 0, camera.screenWidth, camera.screenHeight)
    glBindFramebuffer(GL_FRAMEBUFFER, 0)
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
    glClear(GL_COLOR_BUFFER_BIT)

    textureToScreen.beforeDraw(camera.projection)

    glDisable(GL_DEPTH_TEST)
    glBindTexture(GL_TEXTURE_2D, fbo.texture0)

    quad.draw()

    textureToScreen.afterDraw()
    glBindTexture(GL_TEXTURE_2D, 0)

  }

  var time = 0.0f

  private def update(): Unit = {

    GameWorldSimulator.worldState

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


    simpleMaterial = new SimpleMaterial(gl)
    materialXYRGB = new MaterialXYZRGB(gl)
    textureToScreen = new RenderToScreen(gl)
    bloomShader = new BloomShader(gl)

    quad =
      new ConcreteModelBuilder[VertexXY, VertexXY](
        textureToScreen,
        Array(
          (VertexXY(1.0f, 1.0f), VertexXY(1.0f, 1.0f)),
          (VertexXY(1.0f, -1.0f), VertexXY(1.0f, 0.0f)),
          (VertexXY(-1.0f, -1.0f), VertexXY(0.0f, 0.0f)),

          (VertexXY(1.0f, 1.0f), VertexXY(1.0f, 1.0f)),
          (VertexXY(-1.0f, -1.0f), VertexXY(0.0f, 0.0f)),
          (VertexXY(-1.0f, 1.0f), VertexXY(0.0f, 1.0f))
        )
      ).init()

  }

  def reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int): Unit = {
    camera.screenDims = (width, height)

    if (fbo != null) fbo.delete()
    fbo = new FramebufferObject(width, height, getGL(drawable))

    println(s"reshape($x, $y, $width, $height)")
  }

  def getGL(drawable: GLAutoDrawable): GL4 =
    if (Debug) {
      drawable.setGL(new DebugGL4(drawable.getGL.getGL4)).getGL4
    } else {
      drawable.getGL.getGL4
    }
}
