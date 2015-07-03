package cwinter.codecraft.scalajs

import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.graphics.engine.{Debug, AsciiVisualizer}
import cwinter.codecraft.graphics.model.TheModelCache
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.Rectangle
import cwinter.codecraft.demos.graphics.BlogpostDemo
import cwinter.codecraft.demos.graphics.Chaos
import cwinter.codecraft.demos.physics.TheObjectManager
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.html
import org.scalajs.dom.raw.{WebGLRenderingContext => GL}

import scala.scalajs.js.annotation.JSExport


@JSExport
object Main {
  @JSExport
  def main(target: html.Pre): Unit = {
    println(target)
    TheGameMaster.render = render(target)
    TheGameMaster.runL3vL3()
  }

  @JSExport
  def webgl(canvas: html.Canvas): Unit = {
    def intervalID: Int = canvas.getAttribute("interval-id").toInt
    def reset(): Unit = {
      dom.clearInterval(intervalID)
      TheModelCache.clear()
      Debug.clearDrawAlways()
    }

    TheGameMaster.canvas = canvas
    TheGameMaster.runL3vL3()

    document.getElementById("btn-gameplay").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      reset()
      TheGameMaster.runL3vL3()
    }
    document.getElementById("btn-physics").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      reset()
      TheObjectManager.main(Array())
    }
    document.getElementById("btn-graphics").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      reset()
      BlogpostDemo.main(Array())
    }
  }

  def render(target: html.Pre)(objects: Seq[WorldObjectDescriptor], mapSize: Rectangle): Unit = {
    target.innerHTML = AsciiVisualizer.show(objects, mapSize)
  }
}

