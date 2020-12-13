package cwinter.codecraft.testai

import cwinter.codecraft.core.api._
import cwinter.codecraft.core.game.DroneWorldSimulator

object RunTestAI {
  def main(args: Array[String]): Unit = {
    while (true) {
      val startTime = System.nanoTime()
      var frames = 0L

      for (_ <- 0 to 10) {
        val controllers = Seq(TheGameMaster.destroyerAI(), TheGameMaster.replicatorAI())
        val simulator = new DroneWorldSimulator(TheGameMaster.defaultMap.createGameConfig(controllers))
        simulator.graphicsEnabled = false
        simulator.run(steps = 1000000)
        frames += simulator.timestep
      }

      val elapsed = System.nanoTime() - startTime
      println(s"Ran $frames frames in ${elapsed / 1000 / 1000}ms (${frames * 1000000000 / elapsed}FPS)")
    }
  }
}
