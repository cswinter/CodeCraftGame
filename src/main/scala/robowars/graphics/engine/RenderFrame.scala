package robowars.graphics.engine

import java.awt.BorderLayout
import java.awt.event.{KeyEvent, KeyListener}
import javax.media.opengl._
import javax.media.opengl.GL._
import javax.media.opengl.GL2ES3._
import javax.media.opengl.awt.GLCanvas

import com.jogamp.opengl.util.FPSAnimator

import robowars.graphics.matrices._
import robowars.graphics.model._
import robowars.graphics.primitives._

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
  var models = List.empty[DrawableModel]
  var camera = new Camera2D()
  var fbo: FramebufferObject = null


  canvas.addKeyListener(new KeyListener {
    val moveSpeed = 100
    val zoomSpeed = 0.2f

    override def keyTyped(keyEvent: KeyEvent): Unit = println(s"keyTyped($keyEvent)")

    override def keyPressed(keyEvent: KeyEvent): Unit = keyEvent.getKeyCode match {
      case 37 /* LEFT */ => camera.x -= moveSpeed
      case 39 /* RIGHT */ => camera.x += moveSpeed
      case 38 /* UP */ => camera.y += moveSpeed
      case 40 /* DOWN */ => camera.y -= moveSpeed
      case 33 /* PAGE UP */ => camera.zoom += zoomSpeed
      case 34 /* PAGE DOWN */ => camera.zoom -= zoomSpeed
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


    // draw to texture
    glBindFramebuffer(GL_FRAMEBUFFER, fbo.fbo)
    glViewport(0, 0, camera.screenWidth, camera.screenHeight)

    glClearColor(0.1f, 0, 0.1f, 0.0f)
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)


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

    simpleMaterial = new SimpleMaterial(gl)
    materialXYRGB = new MaterialXYZRGB(gl)
    textureToScreen = new RenderToScreen(gl)
    bloomShader = new BloomShader(gl)

    var modelBuilder: Model = new ConcreteModelBuilder(
      simpleMaterial,
      Array(
        (VertexXY(0, 100), EmptyVertex),
        (VertexXY(100, 0), EmptyVertex),
        (VertexXY(0, -100), EmptyVertex)
      )
    )

    modelBuilder += new ConcreteModelBuilder(
      simpleMaterial,
      Array(
        (VertexXY(0, 100), EmptyVertex),
        (VertexXY(0, -100), EmptyVertex),
        (VertexXY(-100, 0), EmptyVertex)
      )
    )

    triangle = modelBuilder.init()
    models ::= triangle

    val coloredModel = new ConcreteModelBuilder(
      materialXYRGB,
      Array(
        (VertexXYZ(0, 100, 0), ColorRGB(1, 0, 0)),
        (VertexXYZ(100, 0, 0), ColorRGB(0, 1, 0)),
        (VertexXYZ(0, -100, 0), ColorRGB(0, 0, 1))
      )
    )

    models ::= coloredModel.init()


    models ::=
      new Polygon[ColorRGB](7, materialXYRGB)
        .color(ColorRGB(0, 0.1f, 0))
        .zPos(0.5f)
        .scale(100)
        .translate(-500, 100)
        .init()

    models ::=
      new Polygon[ColorRGB](5, materialXYRGB)
        .colorMidpoint(ColorRGB(0, 0.7f, 0))
        .colorOutside(ColorRGB(0, 0.3f, 0))
        .scale(50)
        .translate(-200, -200)
        .init()

    models ::= new Polygon(5, bloomShader)
        .scale(330)
        .color(ColorRGB(0.95f, 0.95f, 0.95f))
        .zPos(-1.5f)
        .init()

    quad =
      new ConcreteModelBuilder[VertexXY, VertexXY](
        textureToScreen,
        Array(
          (VertexXY( 1.0f,  1.0f), VertexXY(1.0f, 1.0f)),
          (VertexXY( 1.0f, -1.0f), VertexXY(1.0f, 0.0f)),
          (VertexXY(-1.0f, -1.0f), VertexXY(0.0f, 0.0f)),

          (VertexXY( 1.0f,  1.0f), VertexXY(1.0f, 1.0f)),
          (VertexXY(-1.0f, -1.0f), VertexXY(0.0f, 0.0f)),
          (VertexXY(-1.0f,  1.0f), VertexXY(0.0f, 1.0f))
        )
        ).init()
  }

  def reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int): Unit = {
    camera.screenDims = (width, height)
    camera.position = (-width / 2, -height / 2)

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
