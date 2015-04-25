package cwinter.codinggame.core

import cwinter.codinggame.util.maths.Rectangle
import cwinter.graphics.application.DrawingCanvas


object TheGameMaster {
  final val WorldSize = Rectangle(-1500, 1500, -1500, 1500)


  def startGame(mothership: DroneController): Unit = {
    val map = WorldMap(WorldSize, 100)
    val simulator = new GameSimulator(map, mothership)
    DrawingCanvas.run(simulator)
  }
}
