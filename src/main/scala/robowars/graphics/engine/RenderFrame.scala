package robowars.graphics.engine

import java.awt.BorderLayout
import java.awt.event.{KeyEvent, KeyListener}
import javax.media.opengl._
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
  var materialXYRGB: MaterialXYRGB = null
  var triangle: DrawableModel = null
  var models = List.empty[DrawableModel]
  var camera = new Camera2D()


  canvas.addKeyListener(new KeyListener {
    override def keyTyped(keyEvent: KeyEvent): Unit = println(s"keyTyped($keyEvent)")

    override def keyPressed(keyEvent: KeyEvent): Unit = keyEvent.getKeyCode match {
      case 37 /* LEFT */ => camera.x -= 0.4f
      case 39 /* RIGHT */ => camera.x += 0.4f
      case 38 /* UP */ => camera.y += 0.4f
      case 40 /* DOWN */ => camera.y -= 0.4f
      case 33 /* PAGE UP */ => camera.zoom += 0.2f
      case 34 /* PAGE DOWN */ => camera.zoom -= 0.2f
      case _ =>
    }//println(s"keyPressed($keyEvent)")

    override def keyReleased(keyEvent: KeyEvent): Unit = println(s"keyReleased($keyEvent)")
  })

  override def display(drawable: GLAutoDrawable): Unit = {
    update()
    render(drawable)
  }

  private def render(drawable: GLAutoDrawable): Unit = {
    val gl = getGL(drawable)
    import gl._


    // set background color
    glClearColor(0.1f, 0, 0.1f, 0.0f)
    glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT)


    for (material <- Seq(simpleMaterial, materialXYRGB)) {
      material.beforeDraw(camera.projection)

      for (model <- models if model.hasMaterial(material))
        model.project(material).draw()

      material.afterDraw()
    }
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
    materialXYRGB = new MaterialXYRGB(gl)

    var modelBuilder: Model = new ModelBuilder[VertexXY, EmptyVertex.type](
      simpleMaterial,
      Array(
        (VertexXY(0, 100), EmptyVertex),
        (VertexXY(100, 0), EmptyVertex),
        (VertexXY(0, -100), EmptyVertex)
      )
    )

    modelBuilder += new ModelBuilder(
      simpleMaterial,
      Array(
        (VertexXY(0, 100), EmptyVertex),
        (VertexXY(0, -100), EmptyVertex),
        (VertexXY(-100, 0), EmptyVertex)
      )
    )

    triangle = modelBuilder.init()
    models ::= triangle

    val coloredModel = new ModelBuilder(
      materialXYRGB,
      Array(
        (VertexXY(0, 100), ColorRGB(1, 0, 0)),
        (VertexXY(100, 0), ColorRGB(0, 1, 0)),
        (VertexXY(0, -100), ColorRGB(0, 0, 1))
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
  }

  def reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int): Unit = {
    camera.screenDims = (width, height)
    println(s"reshape($x, $y, $width, $height)")
  }

  def getGL(drawable: GLAutoDrawable): GL4 =
    if (Debug) {
      drawable.setGL(new DebugGL4(drawable.getGL.getGL4)).getGL4
    } else {
      drawable.getGL.getGL4
    }
}
