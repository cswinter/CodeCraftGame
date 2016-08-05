package cwinter.codecraft.graphics.engine

import java.awt.{Font, TextField}
import javax.media.opengl.GL._
import javax.media.opengl._

import com.jogamp.opengl.util.awt.TextRenderer
import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.{TheCompositeModelBuilderCache, TheModelCache, VBO}
import cwinter.codecraft.util.maths.VertexXY
import org.joda.time.DateTime

import scala.util.Try


private[graphics] class RenderFrame(val gameWorld: Simulator)
    extends GLEventListener {
  val DebugMode = false

  implicit var fbo: FramebufferObject = null
  implicit var renderStack: RenderStack = null
  var camera = new Camera2D
  var textRenderer: TextRenderer = null
  var largeTextRenderer: TextRenderer = null
  val modelCache = new TheModelCache
  val compositeModelBuilderCache = new TheCompositeModelBuilderCache
  var isGL4Supported = false
  lazy val context = new GraphicsContext(renderStack, false, modelCache, compositeModelBuilderCache)

  var cullFaceToggle = false
  val FrametimeSamples = 100
  var frameTimes = scala.collection.mutable.Queue.fill(FrametimeSamples - 1)(new DateTime().getMillis)
  var textField: TextField = null
  var error = false


  override def display(drawable: GLAutoDrawable): Unit = {
    implicit val gl = drawable.getGL
    import gl._

    Material.resetDrawCalls()
    Material.resetModelviewUploads()

    if (cullFaceToggle) glEnable(GL_CULL_FACE)
    else glDisable(GL_CULL_FACE)
    cullFaceToggle = !cullFaceToggle

    // draw to texture
    if (isGL4Supported) {
      glBindFramebuffer(GL_FRAMEBUFFER, fbo.fbo)
      glViewport(0, 0, camera.screenWidth * 2, camera.screenHeight * 2)
    } else {
      glViewport(0, 0, camera.screenWidth, camera.screenHeight)
    }

    if (!error) glClearColor(0.02f, 0.02f, 0.02f, 0.0f)
    else glClearColor(0.1f, 0, 0.0f, 0.0f)
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

    val worldObjects = gameWorld.worldState
    val projection = camera.projection

    for (material <- renderStack.materials) {
      material.beforeDraw(projection)

      for (worldObject <- worldObjects) {
        try {
          worldObject.closedModel(gameWorld.timestep, context).draw(material)
        } catch {
          case t: Throwable =>
            println(s"Encountered error while trying to draw $worldObject.")
            t.printStackTrace()
        }
      }


      material.afterDraw()
    }

    // draw from texture to screen
    renderStack.postDraw(camera)

    renderText(drawable)

    // dispose one-time VBOs
    context.freeTempVBOs(gl)

    // update fps
    val now = new DateTime().getMillis
    frameTimes.enqueue(now)
    val tThen = frameTimes.dequeue()
    val fps = FrametimeSamples * 1000 / (now - tThen)
    textField.setText(
      f"FPS: $fps   " +
      f"TPS: ${gameWorld.measuredFramerate}   " +
      f"Draw calls: ${Material.drawCalls}   " +
      f"Modelview uploads: ${Material.modelviewUploads}   " +
      f"Cached models: ${modelCache.CachedModelCount}   " +
      f"Allocated VBOs: ${VBO.count}   " +
      f"Timestep: ${gameWorld.timestep}   " +
      f"Last cached model: ${modelCache.lastCachedModel}"
    )
  }

  private def renderText(drawable: GLAutoDrawable): Unit = {
    val gl = drawable.getGL
    val width = drawable.getSurfaceWidth
    val height = drawable.getSurfaceHeight
    gl.getGL2.glBindVertexArray(0)
    gl.getGL2.glUseProgram(0)

    textRenderer.beginRendering(width, height)

    for (text <- gameWorld.textModels if !text.largeFont)
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
    for (text <- gameWorld.textModels if text.largeFont)
      renderTextModel(largeTextRenderer, text, width, height)
    largeTextRenderer.endRendering()
  }

  def renderTextModel(renderer: TextRenderer, textModel: TextModel, width: Int, height: Int): Unit = {
    val TextModel(text, xPos, yPos, color, absolutePos, centered, _) = textModel
    renderer.setColor(color.r, color.g, color.b, color.a)
    val bounds = renderer.getBounds(text)
    val worldPos = VertexXY(xPos, yPos)
    val center =
      if (centered) VertexXY(-bounds.getWidth.toFloat / 2, bounds.getHeight.toFloat / 2)
      else VertexXY(0, 0)
    val cameraPos =
      if (absolutePos) VertexXY(xPos * width / 2, yPos * height / 2)
      else (1 / camera.zoomFactor) * (worldPos - VertexXY(camera.x, camera.y))
    val position = cameraPos + center + VertexXY(width / 2, height / 2)
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


  def dispose(arg0: GLAutoDrawable): Unit = context.dispose(arg0.getGL)


  def init(drawable: GLAutoDrawable): Unit = {
    printGLInfo(drawable, drawable.getGL)
    isGL4Supported = performVersionCheck(drawable.getGL)

    getEitherGL(drawable) match {
      case Left(gl2) =>
        renderStack = new JVMGL2RenderStack()(gl2)
      case Right(gl4) =>
        // vsync to prevent screen tearing.
        // seems to work with Ubuntu + i3, but might not be portable
        gl4.setSwapInterval(1)
        fbo = new FramebufferObject()(gl4)
        renderStack = new JVMRenderStack()(gl4, fbo)
    }

    textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 14))
    largeTextRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 90))
    camera.position = (gameWorld.initialCameraPos.x.toInt, gameWorld.initialCameraPos.y.toInt)
    gameWorld.run()
  }

  def printGLInfo(drawable: GLAutoDrawable, gl: GL): Unit = {
    import gl._
    println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities)
    println("INIT GL IS: " + gl.getClass.getName)
    println("GL_VENDOR: " + glGetString(GL.GL_VENDOR))
    println("GL_RENDERER: " + glGetString(GL.GL_RENDERER))
    println("GL_VERSION: " + glGetString(GL.GL_VERSION))
  }

  def performVersionCheck(gl: GL): Boolean = {
    val gl4Supported = Try { gl.getGL4 }.isSuccess
    val gl2Supported = Try { gl.getGL2 }.isSuccess
    if (!gl2Supported && !gl4Supported) {
      println("Failed to obtain OpenGL graphics device :(\n" +
        "CodeCraft requires OpenGL version 2.0 or higher, which your hardware does not seem to support.")
    }
    gl4Supported
  }

  def reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int): Unit = {
    camera.screenDims = (width, height)

    for (gl4 <- getGL4(drawable)) fbo.resize(width, height)(gl4)

    textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 14))
    largeTextRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 90))
  }

  def getGL4(drawable: GLAutoDrawable): Option[GL4] =
    if (isGL4Supported) Some(
      if (DebugMode) new DebugGL4(drawable.getGL.getGL4)
      else drawable.getGL.getGL4
    ) else None

  def getEitherGL(drawable: GLAutoDrawable): Either[GL2, GL4] = {
    if (isGL4Supported) Right(drawable.getGL.getGL4)
    else Left(drawable.getGL.getGL2)
  }
}

