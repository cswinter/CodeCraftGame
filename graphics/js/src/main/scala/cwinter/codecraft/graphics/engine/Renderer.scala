package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.PrimitiveModelBuilder
import cwinter.codecraft.graphics.models.TheWorldObjectModelFactory
import cwinter.codecraft.graphics.worldstate.Simulator
import cwinter.codecraft.util.maths.{ColorRGBA, VertexXY, Rectangle, Vector2}
import org.scalajs.dom
import org.scalajs.dom.raw.{CanvasRenderingContext2D, HTMLCanvasElement, WebGLRenderingContext => GL}
import org.scalajs.dom.{document, html}


class Renderer(
  canvas: html.Canvas,
  gameWorld: Simulator,
  initialCameraPos: Vector2 = Vector2(0, 0)
) {
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
  val textCanvas = document.getElementById("text-canvas").asInstanceOf[HTMLCanvasElement]
  if (textCanvas != null) {
    textCanvas.onkeypress = onkeypressHandler _
    textCanvas.onmousewheel = onmousewheelHandler _
    textCanvas.onmousedown = onmousedownHandler _
    textCanvas.onmouseup = onmouseupHandler _
    textCanvas.onmousemove = onmousemoveHandler _
  }

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

    val textCanvas = document.getElementById("text-canvas").asInstanceOf[HTMLCanvasElement]
    if (textCanvas == null) {
      println("Could not find canvas #text-canvas. Without this, text cannot be rendered.")
    } else {
      val width = textCanvas.clientWidth
      val height = textCanvas.clientHeight
      if (width != camera.screenWidth || height != camera.screenHeight) {
        textCanvas.width = width
        textCanvas.height = height
      }
      val ctx = textCanvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      ctx.clearRect(0, 0, width, height)
      for (text <- Debug.textModels) {
        renderText(ctx, text)
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

  private def renderText(context: CanvasRenderingContext2D, textModel: TextModel): Unit = {
    val TextModel(text, x, y, ColorRGBA(r, g, b, a)) = textModel
    val width = context.canvas.width
    val height = context.canvas.height

    def int(f: Float) = Math.round(255 * f)

    context.fillStyle = s"rgba(${int(r)}, ${int(g)}, ${int(b)}, $a)"
    context.font =  "16px serif bold"
    val textMetric = context.measureText(text)
    val worldPos = VertexXY(x, -y)
    val position = (1 / camera.zoomFactor) * (worldPos - VertexXY(camera.x, -camera.y)) +
      VertexXY(width / 2 - textMetric.width.toFloat / 2, height / 2 + 8)
    context.fillText(text, position.x, position.y)
  }

  private def initGL(): GL = {
    val gl = canvas.getContext("experimental-webgl").asInstanceOf[GL]
    if (gl == null) {
      dom.alert("Could not initialise WebGL. INSERT LINK TO FIX HERE.")
    }
    gl.viewport(0, 0, canvas.width, canvas.height)
    gl
  }
}

