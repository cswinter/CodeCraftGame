package cwinter.codecraft.graphics.engine

import org.scalajs.dom.{document, html}

import scala.scalajs.js.timers.SetIntervalHandle


private[codecraft] object GraphicsEngine {
  private[this] var intervalID: Option[SetIntervalHandle] = None

  def run(simulator: Simulator): Unit = {
    val canvas = document.getElementById("webgl-canvas").asInstanceOf[html.Canvas]
    val renderer = new WebGLRenderer(canvas, simulator)
    intervalID = Some(scala.scalajs.js.timers.setInterval(20.0) {
      renderer.render()
      simulator.run(1)
    })
  }
}

