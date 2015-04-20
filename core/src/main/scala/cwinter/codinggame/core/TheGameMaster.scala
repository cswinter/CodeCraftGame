package cwinter.codinggame.core

import cwinter.codinggame.maths.{Vector2, Rectangle}
import cwinter.graphics.application.DrawingCanvas


object TheGameMaster {
  final val WorldSize = Rectangle(-3000, 3000, -3000, 3000)


  def startGame(mothership: WorldObject): Unit = {
    val map = Map(WorldSize, 100)
    val simulator = new GameSimulator(map)
    DrawingCanvas.run(simulator)
  }
}
