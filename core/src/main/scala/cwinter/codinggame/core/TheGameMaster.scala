package cwinter.codinggame.core

import cwinter.codinggame.maths.Rectangle
import cwinter.graphics.application.DrawingCanvas


object TheGameMaster {
  final val WorldSize = Rectangle(-3000, 3000, -3000, 3000)


  def startGame(mothership: WorldObject): Unit = {
    val simulator = new GameSimulator(WorldSize, Seq())
    DrawingCanvas.run(simulator)
  }
}
