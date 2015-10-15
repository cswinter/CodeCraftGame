package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.worldstate.Simulator
import org.scalajs.dom
import org.scalajs.dom.{html, document}

private[graphics] object GraphicsEngine {
  def run(simulator: Simulator): Unit = {
    val canvas = document.getElementById("webgl-canvas").asInstanceOf[html.Canvas]
    val renderer = new Renderer(canvas, simulator)
    val intervalID = dom.setInterval(() => {
      renderer.render()
      simulator.run(1)
    }, 20)
    canvas.setAttribute("interval-id", intervalID.toString)
  }
}
