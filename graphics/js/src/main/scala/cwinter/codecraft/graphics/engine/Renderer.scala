package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.graphics.model.{VBO, TheModelCache, PrimitiveModelBuilder}
import cwinter.codecraft.graphics.models.TheWorldObjectModelFactory
import cwinter.codecraft.graphics.worldstate.{Simulator, WorldObjectDescriptor}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import org.scalajs.dom.raw.{WebGLRenderingContext => GL}


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
  canvas.onkeypress = (e: dom.KeyboardEvent) => {
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

  def render(): Unit = {
    Material.resetDrawCalls()

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

    // TODO: implement rendering
    //renderText(drawable)

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

  private def initGL(): GL = {
    val gl = canvas.getContext("experimental-webgl").asInstanceOf[GL]
    if (gl == null) {
      dom.alert("Could not initialise WebGL. INSERT LINK TO FIX HERE.")
    }
    gl.viewport(0, 0, canvas.width, canvas.height)
    gl
  }
}

