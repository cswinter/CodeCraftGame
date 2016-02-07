package cwinter.codecraft.graphics.engine

import java.awt.{Font, TextField}
import javax.media.opengl.GL._
import javax.media.opengl._

import com.jogamp.opengl.util.awt.TextRenderer
import cwinter.codecraft.graphics.engine
import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.{PrimitiveModelBuilder, TheModelCache, VBO}
import cwinter.codecraft.graphics.models.TheWorldObjectModelFactory
import cwinter.codecraft.graphics.worldstate.Simulator
import cwinter.codecraft.util.maths.VertexXY
import org.joda.time.DateTime


private[graphics] object RenderFrame extends GLEventListener {
  val DebugMode = false

  var gl: GL4 = null
  implicit var fbo: FramebufferObject = null
  implicit var renderStack: JVMRenderStack = null
  var camera = new Camera2D
  var textRenderer: TextRenderer = null
  var largeTextRenderer: TextRenderer = null

  var cullFaceToggle = false
  val FrametimeSamples = 100
  var frameTimes = scala.collection.mutable.Queue.fill(FrametimeSamples - 1)(new DateTime().getMillis)
  var textField: TextField = null
  var gameWorld: Simulator = null
  var error = false
  var step = 0


  override def display(drawable: GLAutoDrawable): Unit = {
    update()
    render(drawable)
  }

  private def render(drawable: GLAutoDrawable): Unit = {
    implicit val gl = getGL(drawable)
    import gl._

    Material.resetDrawCalls()

    if (cullFaceToggle) glEnable(GL_CULL_FACE)
    else glDisable(GL_CULL_FACE)
    cullFaceToggle = !cullFaceToggle

    // draw to texture
    glBindFramebuffer(GL_FRAMEBUFFER, fbo.fbo)
    glViewport(0, 0, camera.screenWidth * 2, camera.screenHeight * 2)

    if (!error) glClearColor(0.0f, 0, 0.0f, 0.0f)
    else glClearColor(0.1f, 0, 0.0f, 0.0f)
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

    val worldObjects = gameWorld.worldState
    val projection = camera.projection

    for (material <- renderStack.materials) {
      material.beforeDraw(projection)

      for {
        worldObject <- worldObjects ++ engine.Debug.debugObjects
        model = TheWorldObjectModelFactory.generateModel(worldObject, gameWorld.timestep)
      } model.draw(material)


      material.afterDraw()
    }

    // draw to screen
    renderStack.postDraw(camera)

    renderText(drawable)

    // dispose one-time VBOs
    PrimitiveModelBuilder.disposeAll(gl)

    // update fps
    val now = new DateTime().getMillis
    frameTimes.enqueue(now)
    val tThen = frameTimes.dequeue()
    val fps = FrametimeSamples * 1000 / (now - tThen)
    textField.setText(
      f"FPS: $fps   " +
      f"Draw calls: ${Material.drawCalls}   " +
      f"Cached models: ${TheModelCache.CachedModelCount}   " +
      f"Allocated VBOs: ${VBO.count}   " +
      f"Last cached model: ${TheModelCache.lastCachedModel}"
    )
  }

  private def renderText(drawable: GLAutoDrawable): Unit = {
    val gl = getGL(drawable)
    import gl._
    val width = drawable.getSurfaceWidth
    val height = drawable.getSurfaceHeight
    glUseProgram(0)
    glBindVertexArray(0)

    textRenderer.beginRendering(width, height)

    for (text <- Debug.textModels if !text.largeFont)
      renderTextModel(textRenderer, text, width, height)

    textRenderer.setColor(1, 1, 1, 0.7f)
    var yPos = height - 15
    val minHeight = textRenderer.getBounds("A").getHeight.toInt
    for (line <- infoText.split("\n")) {
      textRenderer.draw(line, 0, yPos)
      yPos = yPos - math.max(textRenderer.getBounds(line).getHeight.toInt, minHeight)
    }

    textRenderer.endRendering()

    largeTextRenderer.beginRendering(width, height)
    for (text <- Debug.textModels if text.largeFont)
      renderTextModel(largeTextRenderer, text, width, height)
    largeTextRenderer.endRendering()
  }

  def renderTextModel(renderer: TextRenderer, textModel: TextModel, width: Int, height: Int): Unit = {
    val TextModel(text, xPos, yPos, color, absolutePos, _, centered) = textModel
    renderer.setColor(color.r, color.g, color.b, color.a)
    val bounds = renderer.getBounds(text)
    val worldPos = VertexXY(xPos, yPos)
    var position =
      if (absolutePos) VertexXY(xPos - bounds.getWidth.toFloat / 2, yPos + 8)
      else (1 / camera.zoomFactor) * (worldPos - VertexXY(camera.x, camera.y)) +
        VertexXY(width / 2 - bounds.getWidth.toFloat / 2, height / 2 + bounds.getHeight.toFloat / 2)
    if (centered) position += VertexXY(width / 2, + height / 2)
    renderer.draw(text, position.x.toInt, position.y.toInt)
  }


  private def infoText: String =
    s"""Game speed target: ${gameWorld.framerateTarget}
       |
       |Move camera: WASD, arrow keys
       |Zoom in/out: QE, Page Up/Down
       |${if (gameWorld.isPaused) "Resume game" else "Pause game"}: Spacebar
       |Increase/decrease game speed: F/R
       |Slow mode: P
       |""".stripMargin + gameWorld.additionalInfoText

  private def update(): Unit = {
    step += 1
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
    renderStack = new JVMRenderStack
    textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 14))
    largeTextRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 90))
    camera.position = (gameWorld.initialCameraPos.x.toInt, gameWorld.initialCameraPos.y.toInt)
    gameWorld.run()
  }

  def reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int): Unit = {
    camera.screenDims = (width, height)

    fbo.resize(width, height)(gl)

    textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 14))
    largeTextRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 90))
  }

  def getGL(drawable: GLAutoDrawable): GL4 =
    if (DebugMode) {
      drawable.setGL(new DebugGL4(drawable.getGL.getGL4)).getGL4
    } else {
      drawable.getGL.getGL4
    }
}

