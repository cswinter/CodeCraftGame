package cwinter.codinggame.core

import cwinter.codinggame.core.drone.DroneController
import cwinter.codinggame.util.maths.Rectangle
import cwinter.graphics.application.DrawingCanvas


object TheGameMaster {
  def startGame(mothership1: DroneController, mothership2: DroneController): Unit = {
    val worldSize = Rectangle(-3000, 3000, -3000, 3000)
    val map = WorldMap(worldSize, 10, 10)
    val simulator = new GameSimulator(map, mothership1, mothership2, devEvents)
    DrawingCanvas.run(simulator)
  }


  def runLevel1(mothership1: DroneController): Unit = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val map = WorldMap(worldSize, 100)
    val opponent = new ai.basic.Mothership()
    val simulator = new GameSimulator(map, mothership1, opponent, devEvents)
    DrawingCanvas.run(simulator)
  }


  private var devEvents: Int => Seq[SimulatorEvent] = t => Seq()
  private[cwinter] def setDevEvents(generator: Int => Seq[SimulatorEvent]): Unit = {
    devEvents = generator
  }
}
