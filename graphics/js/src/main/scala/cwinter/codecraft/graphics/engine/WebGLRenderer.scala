package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.{TheCompositeModelBuilderCache, TheModelCache, PrimitiveModelBuilder}
import cwinter.codecraft.util.maths.{ColorRGBA, Rectangle, Vector2, VertexXY}
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLDivElement, WebGLRenderingContext => GL}
import org.scalajs.dom.{document, html}

import scala.scalajs.js

private[codecraft] class WebGLRenderer(
  canvas: html.Canvas,
  gameWorld: Simulator
) extends Renderer {
  implicit val gl = initGL()
  implicit val renderStack = new JSRenderStack()
  val camera = new Camera2D
  val modelCache = new TheModelCache
  val compositeModelBuilderCache = new TheCompositeModelBuilderCache
  lazy val context = new GraphicsContext(renderStack, true, modelCache, compositeModelBuilderCache)
  camera.position = (gameWorld.initialCameraPos.x, gameWorld.initialCameraPos.y)
  camera.screenDims = (canvas.width, canvas.height)
  camera.zoom = gameWorld.initialCameraZoom

  private[this] val keyEventHandler = new KeyEventHandler(gameWorld, camera)
  canvas.onkeypress = onKeyPress _
  canvas.onmousedown = onMouseDown _
  if (document.createElement("div").hasAttribute("onwheel"))
    canvas.addEventListener("onwheel", onMouseWheel _, useCapture = false)
  else if (js.eval("document.onmousewheel !== undefined").asInstanceOf[Boolean]) {
    canvas.onmousewheel = onMouseWheel _
  } else canvas.addEventListener("DOMMouseScroll", onScroll _, useCapture = false)

  canvas.onmouseup = onMouseUp _
  canvas.onmousemove = onMouseMove _

  def onKeyPress(e: dom.KeyboardEvent) = {
    val key = e.keyCode match {
      case 37 => LeftArrow
      case 39 => RightArrow
      case 38 => UpArrow
      case 40 => DownArrow
      case 33 => PageUp
      case 34 => PageDown
      case _ => Letter(e.charCode.toChar)
    }
    keyEventHandler.keypress(key)
  }

  def onMouseWheel(e: dom.WheelEvent): Unit = {
    camera.zoom += 0.001f * e.deltaY.toFloat
  }

  def onScroll(e: dom.UIEvent): Unit = {
    camera.zoom += 0.02f * e.detail
  }

  private[this] var mouseIsDown = false
  def onMouseDown(e: dom.MouseEvent): Unit = {
    mouseIsDown = true
  }

  def onMouseUp(e: dom.MouseEvent): Unit = {
    mouseIsDown = false
  }

  private[this] var xLast = 0.0
  private[this] var yLast = 0.0
  def onMouseMove(e: dom.MouseEvent): Unit = {
    val dx = xLast - e.clientX
    val dy = yLast - e.clientY
    xLast = e.clientX
    yLast = e.clientY
    if ((e.buttons & 7) > 0) {
      camera.x += dx.toFloat * camera.zoomFactor
      camera.y -= dy.toFloat * camera.zoomFactor
    }
  }

  def render(): Unit = {
    Material.resetDrawCalls()
    Material.resetModelviewUploads()

    for (Vector2(x, y) <- gameWorld.debug.cameraOverride)
      camera.position = (x.toFloat, y.toFloat)

    val width = canvas.clientWidth
    val height = canvas.clientHeight
    if (width != camera.screenWidth || height != camera.screenHeight) {
      camera.screenDims = (width, height)
      canvas.width = width
      canvas.height = height
    }

    gl.viewport(0, 0, camera.screenWidth, camera.screenHeight)

    gl.clearColor(0.02, 0.02, 0.02, 1)
    gl.clear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT)

    val (worldObjects, textModels) = gameWorld.dequeueFrame()
    val projectionT = camera.projection.transposed
    val onScreen =
      Rectangle(
        camera.x - camera.zoomFactor * 0.5f * camera.screenWidth,
        camera.x + camera.zoomFactor * 0.5f * camera.screenWidth,
        camera.y - camera.zoomFactor * 0.5f * camera.screenHeight,
        camera.y + camera.zoomFactor * 0.5f * camera.screenHeight
      )

    var mcurr = renderStack.materials
    while (mcurr != Nil) {
      val material = mcurr.head
      material.beforeDraw(projectionT)

      var objcurr = worldObjects
      while (objcurr != Nil) {
        val modelDescriptor = objcurr.head
        if (modelDescriptor.intersects(onScreen)) {
          modelDescriptor.closedModel(gameWorld.timestep, context).draw(material)
        }

        objcurr = objcurr.tail
      }

      material.afterDraw()
      mcurr = mcurr.tail
    }

    val textDiv = document.getElementById("text-container").asInstanceOf[HTMLDivElement]
    val textTestDiv = document.getElementById("text-test-container").asInstanceOf[HTMLDivElement]
    if (textDiv == null || textTestDiv == null) {
      println(
        "Could not find div#text-container and div#text-test-container. Without this, text cannot be rendered.")
    } else if (textModels.nonEmpty || textDiv.innerHTML != "") {
      textTestDiv.innerHTML = """<div id="large-text-dim-test"></div><div id="small-text-dim-test"></div>"""
      textDiv.innerHTML = ""
      for (text <- textModels) renderText(textDiv, text, width, height)
      if (gameWorld.isPaused) {
        renderText(
          textDiv,
          TextModel(
            "Game Paused. Press SPACEBAR to resume.",
            width / 2,
            height / 2,
            ColorRGBA(1, 1, 1, 1),
            absolutePos = true
          ),
          width,
          height
        )
      }
      textTestDiv.innerHTML = ""
    }

    // dispose one-time VBOs
    //context.freeTempVBOs(gl)
  }

  private def renderText(container: HTMLDivElement, textModel: TextModel, width: Int, height: Int): Unit = {
    val TextModel(text, x, y, ColorRGBA(r, g, b, a), absolutePos, centered, largeFont) = textModel
    def int(f: Float) = Math.round(255 * f)

    val position =
      if (absolutePos) screenToBrowserCoords(x, y, width, height)
      else worldToBrowserCoords(x, y, width, height)

    val (textWidth, textHeight) =
      if (centered) {
        val testDivID = if (largeFont) "large-text-dim-test" else "small-text-dim-test"
        val testDiv = document.getElementById(testDivID).asInstanceOf[HTMLDivElement]
        testDiv.innerHTML = textModel.text
        (testDiv.clientWidth + 1, testDiv.clientHeight + 1)
      } else (0, 0)

    if (!absolutePos && (position.x - textWidth / 2 < 0 ||
        position.y - textHeight < 0 ||
        position.x > width + textWidth / 2 ||
        position.y > height + textHeight / 2)) return

    val textElem = document.createElement("div").asInstanceOf[HTMLDivElement]
    textElem.className = if (largeFont) "large-floating-text" else "floating-text"
    textElem.style.color = s"rgba(${int(r)}, ${int(g)}, ${int(b)}, $a)"
    textElem.innerHTML = text
    textElem.style.left = s"${position.x.toInt - textWidth / 2}px"
    textElem.style.top = s"${position.y.toInt - textHeight / 2}px"
    container.appendChild(textElem)
  }

  private def worldToBrowserCoords(x: Float, y: Float, width: Int, height: Int): VertexXY = {
    val worldPos = VertexXY(x, -y)
    val cameraPos = (1 / camera.zoomFactor) * (worldPos - VertexXY(camera.x, -camera.y)) +
      VertexXY(width / 2f, height / 2f)
    cameraPos
  }

  private def screenToBrowserCoords(x: Float, y: Float, width: Int, height: Int): VertexXY = {
    require(-1 <= x); require(x <= 1); require(-1 <= y); require(y <= 1)
    VertexXY(x * width / 2f, -y * height / 2f) + VertexXY(width / 2f, height / 2f)
  }

  private def initGL(): GL = {
    val gl = canvas.getContext("experimental-webgl").asInstanceOf[GL]
    if (gl == null) {
      dom.window.alert("Could not initialise WebGL. Visit https://get.webgl.org for further info.")
    }
    gl.viewport(-1, 1, canvas.width, canvas.height)
    gl
  }

  def dispose(): Unit = context.dispose(gl)
}
