package cwinter.codecraft.graphics.engine

import org.scalajs.dom
import org.scalajs.dom.{document, html}


private[codecraft] object GraphicsEngine {
  def run(simulator: Simulator): Unit = {
    val canvas = document.getElementById("webgl-canvas").asInstanceOf[html.Canvas]
    val renderer = new WebGLRenderer(canvas, simulator)
    val intervalID = dom.setInterval(() => {
      renderer.render()
      simulator.run(1)
    }, 20)
    canvas.setAttribute("interval-id", intervalID.toString)
  }
}
