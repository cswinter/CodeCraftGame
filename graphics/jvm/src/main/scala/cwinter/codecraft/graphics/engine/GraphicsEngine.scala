package cwinter.codecraft.graphics.engine

import cwinter.codecraft.graphics.application.DrawingCanvas
import cwinter.codecraft.graphics.worldstate.Simulator

private[codecraft] object GraphicsEngine {
  def run(simulator: Simulator): Unit = {
    DrawingCanvas.run(simulator)
  }
}
