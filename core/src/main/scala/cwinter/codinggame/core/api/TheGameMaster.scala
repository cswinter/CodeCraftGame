package cwinter.codinggame.core.api

import cwinter.codinggame.core.{GameSimulator, SimulatorEvent, WorldMap, ai}
import cwinter.codinggame.graphics.application.DrawingCanvas
import cwinter.codinggame.util.maths.{Vector2, Rectangle}


object TheGameMaster {
  def startGame(mothership1: DroneController, mothership2: DroneController): Unit = {
    val worldSize = Rectangle(-5000, 5000, -3000, 3000)
    val resourceClusters = Seq(
      (20, 1), (20, 1), (20, 1), (20, 1),
      (20, 2), (20, 2),
      (15, 3), (15, 3),
      (15, 4), (15, 4),
      (20, 5)
    )
    val map = WorldMap(worldSize, resourceClusters, Seq(Vector2(3000, 750), Vector2(-3000, -750)))
    val simulator = new GameSimulator(map, mothership1, mothership2, devEvents)
    DrawingCanvas.run(simulator)
  }


  def runLevel1(mothership1: DroneController): Unit = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val map = WorldMap(worldSize, 100, Seq(Vector2(1000, 200), Vector2(-1000, -200)))
    val opponent = new ai.basic.Mothership()
    val simulator = new GameSimulator(map, mothership1, opponent, devEvents)
    DrawingCanvas.run(simulator)
  }


  private var devEvents: Int => Seq[SimulatorEvent] = t => Seq()
  private[cwinter] def setDevEvents(generator: Int => Seq[SimulatorEvent]): Unit = {
    devEvents = generator
  }
}
