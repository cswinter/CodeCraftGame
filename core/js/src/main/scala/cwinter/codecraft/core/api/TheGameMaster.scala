package cwinter.codecraft.core.api

import cwinter.codecraft.core.DroneWorldSimulator

object TheGameMaster extends GameMasterLike {
  override def run(simulator: DroneWorldSimulator): Unit = {
    println("Starting simulator...")
    for (t <- 0 to 100) {
      println("t = " + t)
      println("object count: " + simulator.worldState.length)
      simulator.run(1)
    }
    println("Success")
  }
}
