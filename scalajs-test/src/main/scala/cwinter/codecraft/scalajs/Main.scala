package cwinter.codecraft.scalajs

import cwinter.codecraft.core.DroneWorldSimulator
import cwinter.codecraft.core.api.{DroneControllerBase, TheGameMaster}
import cwinter.codecraft.demos.graphics.BlogpostDemo
import cwinter.codecraft.demos.physics.TheObjectManager
import cwinter.codecraft.graphics.engine.{AsciiVisualizer, Debug}
import cwinter.codecraft.graphics.model.TheModelCache
import cwinter.codecraft.graphics.worldstate._
import cwinter.codecraft.util.maths.Rectangle
import org.scalajs.dom
import org.scalajs.dom.{document, html}

import scala.scalajs.js.annotation.JSExport


@JSExport
object Main {
  @JSExport
  def webgl(canvas: html.Canvas): Unit = {
    def intervalID: Int = canvas.getAttribute("interval-id").toInt
    def reset(): Unit = {
      dom.clearInterval(intervalID)
      TheModelCache.clear()
      Debug.clearDrawAlways()
    }
    TheGameMaster.canvas = canvas
    TheGameMaster.outputFPS = true
    run(TheGameMaster.replicatorAI(), TheGameMaster.replicatorAI())

    document.getElementById("btn-gameplay").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      reset()
      run(TheGameMaster.replicatorAI(), TheGameMaster.replicatorAI())
    }/*
    document.getElementById("btn-physics").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      reset()
      TheObjectManager.main(Array())
    }
    document.getElementById("btn-graphics").asInstanceOf[html.Button].onclick = (e: dom.Event) => {
      reset()
      BlogpostDemo.main(Array())
    }*/
  }

  def run(m1: DroneControllerBase, m2: DroneControllerBase): Unit = {
    val simulator = new DroneWorldSimulator(
      TheGameMaster.defaultMap,
      Seq(m1, m2),
      t => Seq.empty
    )
    TheGameMaster.run(simulator)
  }

  def render(target: html.Pre)(objects: Seq[ModelDescriptor[_]], mapSize: Rectangle): Unit = {
    target.innerHTML = AsciiVisualizer.show(objects, mapSize)
  }
}

