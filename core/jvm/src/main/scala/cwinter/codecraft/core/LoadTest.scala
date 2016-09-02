package cwinter.codecraft.core

import java.util.concurrent.Executors

import cwinter.codecraft.core.api.{DroneControllerBase, TheGameMaster}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

private[core] object LoadTest {
  implicit val executionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1000))
  val MaxGames = 1000
  val ServerURL = "localhost"
  val Interval = 1

  def main(args: Array[String]): Unit = {
    for (i <- 0 to MaxGames) {
      spawnGame(i)
      Thread.sleep(Interval * 1000)
    }
  }

  def spawnGame(i: Int): Unit = {
    println(s"Spawning Game $i")
    spawnConnection(TheGameMaster.replicatorAI(), s"$i-rep")
    spawnConnection(TheGameMaster.destroyerAI(), s"$i-des")
  }

  def spawnConnection(ai: => DroneControllerBase, id: String): Unit =
    TheGameMaster.prepareMultiplayerGame(ServerURL, ai).onComplete {
      case Success(game) =>
        println(s"Connection Success for $id")
        game.run()
      case Failure(x) =>
        println(s"Connection Failure($x) for $id")
    }
}
