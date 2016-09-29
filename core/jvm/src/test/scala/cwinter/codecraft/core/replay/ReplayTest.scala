package cwinter.codecraft.core.replay

import cwinter.codecraft.core.TestUtils
import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.core.game.DroneWorldSimulator
import org.scalatest.FlatSpec

class ReplayTest extends FlatSpec {
  "A replayer" should "work" in {
    val timesteps = 5000
    val recorder = new StringReplayRecorder
    val simulator = new DroneWorldSimulator(
      TheGameMaster.defaultMap.createGameConfig(
        Seq(TheGameMaster.replicatorAI(), TheGameMaster.destroyerAI())),
      forceReplayRecorder = Some(recorder)
    )
    val canonical = TestUtils.runAndRecord(simulator, timesteps)

    val replaySimulator = TheGameMaster.createReplaySimulator(recorder.replayString.get)
    val fromReplay = TestUtils.runAndRecord(replaySimulator, timesteps)

    TestUtils.assertEqual(canonical, fromReplay, simulator, replaySimulator)
  }
}
