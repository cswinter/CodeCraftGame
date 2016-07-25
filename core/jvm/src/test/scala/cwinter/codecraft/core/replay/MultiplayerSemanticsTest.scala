package cwinter.codecraft.core.replay

import cwinter.codecraft.core.TestUtils
import cwinter.codecraft.core.ai.deterministic.DeterministicMothership
import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.core.game.DroneWorldSimulator
import cwinter.codecraft.core.multiplayer.Server
import cwinter.codecraft.util.maths.GlobalRNG
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration._


class MultiplayerSemanticsTest extends FlatSpec {
  val timesteps = 7500
  val seed = 42
  val tickPeriod = 10
  val mapSeed: Option[Int] = None
  def ai() = new DeterministicMothership(seed)
  mapSeed.foreach(GlobalRNG.seed = _)

  s"A multiplayer game (map ${GlobalRNG.seed})" should "yield the same output as an identical singleplayer game" in {
    DroneWorldSimulator.enableDetailedLogging()
    TestUtils.runAndCompare(singleplayerGame(), multiplayerGame(), timesteps)
  }

  def multiplayerGame(): DroneWorldSimulator = {
    new Thread {
      override def run(): Unit = {
        Server.spawnServerInstance2(seed, TheGameMaster.level1Map, displayGame = false)
      }
    }.start()
    Thread.sleep(1000, 0)

    def connectClient() = Await.result(
      TheGameMaster.prepareMultiplayerGame("localhost", ai()),
      2.seconds
    )
    val client1 = connectClient()
    client1.framerateTarget = 1001
    client1.run()
    connectClient()
  }

  def singleplayerGame(): DroneWorldSimulator = {
    val singleplayerGame = new DroneWorldSimulator(
      TheGameMaster.level1Map,
      Seq(ai(), ai()),
      t => Seq.empty,
      rngSeed = seed
    )
    singleplayerGame
  }
}

