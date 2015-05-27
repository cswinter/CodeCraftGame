package cwinter.codinggame.core.api

import cwinter.codinggame.core.{DroneWorldSimulator, SimulatorEvent, WorldMap, ai}
import cwinter.codinggame.graphics.application.DrawingCanvas
import cwinter.codinggame.util.maths.{Vector2, Rectangle}


object TheGameMaster {
  def startGame(mothership1: DroneController, mothership2: DroneController): Unit = {
    val worldSize = Rectangle(-4000, 4000, -2500, 2500)
    val resourceClusters = Seq(
      (20, 1), (20, 1), (20, 1), (20, 1),
      (20, 2), (20, 2),
      (15, 3), (15, 3),
      (15, 4), (15, 4)
    )
    val map = WorldMap(worldSize, resourceClusters, Seq(Vector2(2500, 500), Vector2(-2500, -500)))
    val simulator = new DroneWorldSimulator(map, mothership1, mothership2, devEvents)
    DrawingCanvas.run(simulator)
  }


  def runLevel1(mothership1: DroneController): Unit = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val map = WorldMap(worldSize, 100, Seq(Vector2(1000, 200), Vector2(-1000, -200)))
    val opponent = new ai.basic.Mothership()
    val simulator = new DroneWorldSimulator(map, mothership1, opponent, devEvents)
    DrawingCanvas.run(simulator)
  }


  private var devEvents: Int => Seq[SimulatorEvent] = t => Seq()
  private[cwinter] def setDevEvents(generator: Int => Seq[SimulatorEvent]): Unit = {
    devEvents = generator
  }
}
