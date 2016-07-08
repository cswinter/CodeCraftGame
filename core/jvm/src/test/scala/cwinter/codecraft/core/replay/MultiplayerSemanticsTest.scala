package cwinter.codecraft.core.replay

import cwinter.codecraft.core.ai.deterministic.DeterministicMothership
import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.core.game.{Settings, DroneWorldSimulator}
import cwinter.codecraft.core.multiplayer.Server
import cwinter.codecraft.core.TestUtils
import org.scalatest.FlatSpec

import scala.concurrent.Await
import scala.concurrent.duration._


class MultiplayerSemanticsTest extends FlatSpec {
  val timesteps = 7500
  val seed = 42
  val tickPeriod = 10
  def ai() = new DeterministicMothership(seed)

  "A multiplayer game" should "yield the same output as an identical singleplayer game" in {
    val singleplayerOutput = runSingleplayerGame()
    val multiplayerOutput = runMultiplayerGame()

    TestUtils.assertEqual(singleplayerOutput, multiplayerOutput, "", tickPeriod)
  }

  def runMultiplayerGame(): TestUtils.GameRecord = {
    new Thread {
      override def run(): Unit = {
        Server.spawnServerInstance2(seed, TheGameMaster.level1Map)
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
    TestUtils.runAndRecord(connectClient(), timesteps)
  }

  def runSingleplayerGame(): TestUtils.GameRecord = {
    val singleplayerGame = new DroneWorldSimulator(
      TheGameMaster.level1Map,
      Seq(ai(), ai()),
      t => Seq.empty,
      rngSeed = seed
    )
    TestUtils.runAndRecord(singleplayerGame, timesteps)
  }
}

