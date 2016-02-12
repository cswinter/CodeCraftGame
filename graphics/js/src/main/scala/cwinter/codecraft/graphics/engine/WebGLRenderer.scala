package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.PrimitiveModelBuilder
import cwinter.codecraft.graphics.models.TheWorldObjectModelFactory
import cwinter.codecraft.graphics.worldstate.Simulator
import cwinter.codecraft.util.maths.{ColorRGBA, VertexXY, Rectangle, Vector2}
import org.scalajs.dom
import org.scalajs.dom.raw.{WebGLRenderingContext => GL, HTMLDivElement, CanvasRenderingContext2D, HTMLCanvasElement}
import org.scalajs.dom.{document, html}


private[codecraft] class WebGLRenderer(
  canvas: html.Canvas,
  gameWorld: Simulator,
  initialCameraPos: Vector2 = Vector2(0, 0)
) extends Renderer {
  implicit val gl = initGL()
  implicit val renderStack = new JSRenderStack()
  val camera = new Camera2D
  camera.position = (initialCameraPos.x.toFloat, initialCameraPos.y.toFloat)
  camera.screenDims = (canvas.width, canvas.height)

  private[this] val keyEventHandler = new KeyEventHandler(gameWorld, camera)
  canvas.onkeypress = onkeypressHandler _
  canvas.onmousewheel = onmousewheelHandler _
  canvas.onmousedown = onmousedownHandler _
  canvas.onmouseup = onmouseupHandler _
  canvas.onmousemove = onmousemoveHandler _


  def onkeypressHandler(e: dom.KeyboardEvent) = {
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

  def onmousewheelHandler(e: dom.WheelEvent): Unit = {
    camera.zoom += 0.001f * e.deltaY.toFloat
  }

  private[this] var mouseIsDown = false
  def onmousedownHandler(e: dom.MouseEvent): Unit = {
    mouseIsDown = true
  }

  def onmouseupHandler(e: dom.MouseEvent): Unit = {
    mouseIsDown = false
  }

  private[this] var xLast = 0.0
  private[this] var yLast = 0.0
  def onmousemoveHandler(e: dom.MouseEvent): Unit = {
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

    for (Vector2(x, y) <- Debug.cameraOverride)
      camera.position = (x.toFloat, y.toFloat)

    val width = canvas.clientWidth
    val height = canvas.clientHeight
    if (width != camera.screenWidth || height != camera.screenHeight) {
      camera.screenDims = (width, height)
      canvas.width = width
      canvas.height = height
    }

    gl.viewport(0, 0, camera.screenWidth, camera.screenHeight)

    gl.clearColor(0, 0, 0, 1)
    gl.clear(GL.COLOR_BUFFER_BIT | GL.DEPTH_BUFFER_BIT)

    val worldObjects = gameWorld.worldState
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

      var objcurr = worldObjects ++ Debug.debugObjects
      while (objcurr != Nil) {
        val worldObject = objcurr.head
        if (worldObject.intersects(onScreen)) {
          val model = TheWorldObjectModelFactory.generateModel(worldObject, gameWorld.timestep)
          model.draw(material)
        }

        objcurr = objcurr.tail
      }

      material.afterDraw()
      mcurr = mcurr.tail
    }

    val textDiv = document.getElementById("text-container").asInstanceOf[HTMLDivElement]
    if (textDiv == null) {
      println("Could not find div #text-container. Without this, text cannot be rendered.")
    } else {
      textDiv.innerHTML = ""
      for (text <- Debug.textModels) {
        renderText(textDiv, text, width, height)
      }
      if (gameWorld.isPaused) {
        renderText(
          textDiv,
          TextModel(
            "Game Paused. Press SPACEBAR to resume.",
            width / 2, height / 2,
            ColorRGBA(1, 1, 1, 1),
            absolutePos = true
          ),
          width,
          height
        )
      }
    }

    // dispose one-time VBOs
    PrimitiveModelBuilder.disposeAll(gl)

    // TODO: port to js?
    // update fps
    /*val now = new DateTime().getMillis
    frameTimes.enqueue(now)
    val tThen = frameTimes.dequeue()
    val fps = FrametimeSamples * 1000 / (now - tThen)
    textField.setText(
      f"FPS: $fps   " +
        f"Draw calls: ${Material.drawCalls}   " +
        f"Cached models: ${TheModelCache.CachedModelCount}   " +
        f"Allocated VBOs: ${VBO.count}   " +
        f"Last cached model: ${TheModelCache.lastCachedModel}"
    )*/
  }

  private def renderText(container: HTMLDivElement, textModel: TextModel, width: Int, height: Int): Unit = {
    val TextModel(text, x, y, ColorRGBA(r, g, b, a), absolutePos, largeFont, centered) = textModel
    def int(f: Float) = Math.round(255 * f)


    val position =
      if (absolutePos) screenToBrowserCoords(x, y, width, height)
      else worldToBrowserCoords(x, y, width, height)

    // FIXME: need to know bounds
    if (position.x < 0 || position.y < 0 || position.x > width || position.y > height) {
      println(s"Discarded (${position.x}, ${position.y}):  $textModel")
      return
    }

    val textElem = document.createElement("div").asInstanceOf[HTMLDivElement]
    textElem.className = if (largeFont) "large-floating-text" else "floating-text"
    textElem.style.color = s"rgba(${int(r)}, ${int(g)}, ${int(b)}, $a)"
    textElem.innerHTML = text
    textElem.style.left = s"${position.x.toInt}px"
    textElem.style.top = s"${position.y.toInt}px"
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

  private def renderText(context: CanvasRenderingContext2D, textModel: TextModel): Unit = {
    val TextModel(text, x, y, ColorRGBA(r, g, b, a), absolutePos, largeFont, centered) = textModel
    val width = context.canvas.width
    val height = context.canvas.height

    def int(f: Float) = Math.round(255 * f)

    context.fillStyle = s"rgba(${int(r)}, ${int(g)}, ${int(b)}, $a)"
    context.font =  if (largeFont) "90px serif bold" else "16px serif bold"
    val textMetric = context.measureText(text)
    val worldPos = VertexXY(x, -y)
    var position =
      if (absolutePos) VertexXY(x - textMetric.width.toFloat / 2, y + 8)
      else {
        (1 / camera.zoomFactor) * (worldPos - VertexXY(camera.x, -camera.y)) +
          VertexXY(width / 2 - textMetric.width.toFloat / 2, height / 2 + 8)
      }
    if (centered) position += VertexXY(width / 2, + height / 2)

    context.fillText(text, position.x, position.y)
  }

  private def initGL(): GL = {
    val gl = canvas.getContext("experimental-webgl").asInstanceOf[GL]
    if (gl == null) {
      dom.alert("Could not initialise WebGL. INSERT LINK TO FIX HERE.")
    }
    gl.viewport(-1, 1, canvas.width, canvas.height)
    gl
  }
}

