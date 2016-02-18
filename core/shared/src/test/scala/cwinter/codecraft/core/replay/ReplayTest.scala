package cwinter.codecraft.core.replay

import cwinter.codecraft.core.DroneWorldSimulator
import cwinter.codecraft.core.api.TheGameMaster
import cwinter.codecraft.graphics.worldstate.{DroneDescriptor, ModelDescriptor}
import org.scalatest.FlatSpec

import scala.collection.mutable.ArrayBuffer


class ReplayTest extends FlatSpec {
  "A replayer" should "work" in {
    val recorder = new StringReplayRecorder
    val simulator = new DroneWorldSimulator(
      TheGameMaster.defaultMap(),
      Seq(TheGameMaster.replicatorAI(), TheGameMaster.destroyerAI()),
      t => Seq.empty,
      forceReplayRecorder = Some(recorder)
    )

    val timesteps = 5000

    val canonical = runAndRecord(simulator, timesteps)

    val replaySimulator = TheGameMaster.createReplaySimulator(recorder.replayString.get)

    val fromReplay = runAndRecord(replaySimulator, timesteps)

    for (t <- 0 to timesteps) {
      if (canonical(t) != fromReplay(t)) {
        println("Full Replay:")
        println(recorder.replayString.get)
        println(s"Replay diverged at t=$t.")
        println()
        println("Expected:")
        println(canonical(t))
        println()
        println("Actual:")
        println(fromReplay(t))
        println()
        println("In expected but not in actual:")
        (canonical(t) -- fromReplay(t)).foreach(println)
        println()
        println("In actual but not in expected:")
        (fromReplay(t) -- canonical(t)).foreach(println)
        assert(false)
      }
    }
  }


  def runAndRecord(droneWorldSimulator: DroneWorldSimulator, timesteps: Int): IndexedSeq[Set[ModelDescriptor]] = {
    val snapshots = ArrayBuffer.empty[Set[ModelDescriptor]]

    for (i <- 0 to timesteps) {
      droneWorldSimulator.run(1)
      snapshots.append(droneWorldSimulator.worldState.filter{
        case ModelDescriptor(a, b) => b.isInstanceOf[DroneDescriptor]
      }.toSet)
    }

    snapshots
  }
}

