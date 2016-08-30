package cwinter.codecraft.core

import cwinter.codecraft.core.api.{DroneControllerBase, TheGameMaster}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

private[core] object LoadTest {
  val MaxGames = 100
  val ServerURL = "localhost"
  val Interval = 10

  def main(args: Array[String]): Unit = {
    for (_ <- 0 to MaxGames) {
      spawnGame()
      Thread.sleep(Interval * 1000)
    }
  }

  def spawnGame(): Unit = {
    println("Spawning Game")
    spawnConnection(TheGameMaster.replicatorAI())
    spawnConnection(TheGameMaster.destroyerAI())
  }

  def spawnConnection(ai: => DroneControllerBase): Unit = async {
    val game = await { TheGameMaster.prepareMultiplayerGame(ServerURL, ai) }
    game.run()
  }
}
