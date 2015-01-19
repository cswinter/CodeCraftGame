package robowars.graphics.engine

import java.awt.TextField

import javax.media.opengl._
import javax.media.opengl.GL._

import org.joda.time.DateTime

import robowars.graphics.model._
import robowars.simulation.GameWorldSimulator



object RenderFrame extends GLEventListener {
  val Debug = false

  val FrametimeSamples = 100
  var frameTimes = scala.collection.mutable.Queue.fill(FrametimeSamples - 1)(new DateTime().getMillis)

  var textField: TextField = null
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


  override def display(drawable: GLAutoDrawable): Unit = {
    update()
    render(drawable)
  }

  private def render(drawable: GLAutoDrawable): Unit = {
    val gl = getGL(drawable)
    import gl._

    //textfield.setText("asdf;lkjadsf")



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


    // update fps
    val now = new DateTime().getMillis
    frameTimes.enqueue(now)
    val then = frameTimes.dequeue()
    val fps = FrametimeSamples * 1000 / (now - then)
    textField.setText(s"FPS: $fps")
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
