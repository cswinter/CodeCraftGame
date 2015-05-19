package cwinter.codinggame.graphics.engine

import java.awt.{Font, TextField}

import javax.media.opengl._
import javax.media.opengl.GL._

import com.jogamp.opengl.util.awt.TextRenderer
import cwinter.codinggame.graphics.engine
import cwinter.codinggame.graphics.materials.Material
import cwinter.codinggame.graphics.matrices.IdentityMatrix4x4
import cwinter.codinggame.graphics.model.{VBO, TheModelCache, PrimitiveModelBuilder}
import cwinter.codinggame.graphics.models.TheWorldObjectModelFactory
import cwinter.codinggame.util.maths.VertexXY
import cwinter.codinggame.worldstate.GameWorld
import org.joda.time.DateTime


object RenderFrame extends GLEventListener {
  val DebugMode = false

  private[this] var paused = false

  def togglePause(): Unit = paused = !paused

  var gl: GL4 = null
  implicit var fbo: FramebufferObject = null
  implicit var renderStack: RenderStack = null
  var camera = new Camera2D
  var textRenderer: TextRenderer = null

  var cullFaceToggle = false
  val FrametimeSamples = 100
  var frameTimes = scala.collection.mutable.Queue.fill(FrametimeSamples - 1)(new DateTime().getMillis)
  var textField: TextField = null
  var gameWorld: GameWorld = null
  var error = false
  var t = true
  var slowMode = false
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

    for (material <- renderStack.materials) {
      material.beforeDraw(camera.projection)

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
    PrimitiveModelBuilder.disposeAll()

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

    for (TextModel(text, xPos, yPos, color) <- Debug.textModels) {
      textRenderer.setColor(color.r, color.g, color.b, color.a)
      val bounds = textRenderer.getBounds(text)
      val projection = IdentityMatrix4x4
      val worldPos = VertexXY(xPos - bounds.getWidth.toFloat / 2, yPos + bounds.getHeight.toFloat / 2)
      val position = (1 / camera.zoomFactor) * (worldPos - VertexXY(camera.x, camera.y)) +
        VertexXY(width / 2, height / 2)
      textRenderer.draw(text, position.x.toInt, position.y.toInt)
    }

    textRenderer.endRendering()
  }

  private def update(): Unit = {
    if (!paused) {
      if (!slowMode || step % 10 == 0) {
        if (t) {
          try {
            engine.Debug.clear()
            gameWorld.update()
          } catch {
            case e: Exception =>
              println(e)
              e.getStackTrace.foreach(println)
              paused = true
              error = true
          }
        }
        t = !t
      }
      step += 1
    }
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
    textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 14))
  }

  def reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int): Unit = {
    camera.screenDims = (width, height)

    fbo.resize(width, height)(gl)

    println(s"reshape($x, $y, $width, $height)")
  }

  def getGL(drawable: GLAutoDrawable): GL4 =
    if (DebugMode) {
      drawable.setGL(new DebugGL4(drawable.getGL.getGL4)).getGL4
    } else {
      drawable.getGL.getGL4
    }
}
