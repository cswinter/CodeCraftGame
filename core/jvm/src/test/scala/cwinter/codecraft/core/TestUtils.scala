package cwinter.codecraft.core

import cwinter.codecraft.core.game.DroneWorldSimulator
import cwinter.codecraft.core.graphics.DroneModel
import cwinter.codecraft.core.objects.drone.{UnstructuredEvent, DamageTaken, Command, Position}
import cwinter.codecraft.graphics.engine.ModelDescriptor
import cwinter.codecraft.util.maths.Vector2
import org.scalatest.Matchers

import scala.collection.mutable.ArrayBuffer


object TestUtils extends Matchers {
  type Snapshot = Set[ModelDescriptor[_]]
  type GameRecord = IndexedSeq[Snapshot]


  def runAndRecord(simulator: DroneWorldSimulator, timesteps: Int): GameRecord = {
    val snapshots = ArrayBuffer.empty[Set[ModelDescriptor[_]]]

    simulator.run(1)
    for (i <- 0 to timesteps / simulator.tickPeriod) {
      val snapshot = runSinglePeriod(simulator)
      snapshots.append(snapshot)
    }

    snapshots
  }

  def runAndCompare(simulator1: DroneWorldSimulator,
                    simulator2: DroneWorldSimulator,
                    timesteps: Int): Unit = {
    assert(simulator1.tickPeriod === simulator2.tickPeriod)
    simulator1.run(1)
    simulator2.run(1)
    for (t <- 0 to timesteps / simulator1.tickPeriod) {
      val snapshot1 = runSinglePeriod(simulator1)
      val snapshot2 = runSinglePeriod(simulator2)
      if (snapshot1 != snapshot2) {
        showMismatchAndLog(snapshot1, snapshot2, simulator1, simulator2, simulator1.timestep)
        fail(s"Mismatch detected at time ${simulator1.timestep}")
      }
    }
  }

  private def runSinglePeriod(simulator: DroneWorldSimulator): Snapshot = {
    simulator.run(simulator.tickPeriod)
    assert(simulator.timestep % simulator.tickPeriod === 0)
    simulator.worldState.filter(_.objectDescriptor.isInstanceOf[DroneModel]).toSet
  }

  def assertEqual(run1: GameRecord, run2: GameRecord, debugInfo: String, tickPeriod: Int = 1): Unit = {
    require(run1.size == run2.size)
    val timesteps = run1.size
    for (t <- 0 until timesteps) {
      if (run1(t) != run2(t)) {
        println(debugInfo)
        println(s"Games diverged at t=${t * tickPeriod}\n")
        showMismatch(run1(t), run2(t))
      }
    }
  }

  private def showMismatch(ss1: Snapshot, ss2: Snapshot): Unit = {
    println("Expected:")
    println(ss1)
    println()
    println("Actual:")
    println(ss2)
    println()
    println("In expected but not in actual:")
    (ss1 -- ss2).foreach(println)
    println()
    println("In actual but not in expected:")
    (ss2 -- ss1).foreach(println)
    assert(false)
  }

  private def showMismatchAndLog(ss1: Snapshot,
                                 ss2: Snapshot,
                                 sim1: DroneWorldSimulator,
                                 sim2: DroneWorldSimulator,
                                 timestep: Int): Unit = {
    println(s"Mismatch found at t=$timestep")
    val missing = ss1 -- ss2
    println("In expected but not in actual:")
    missing.foreach(println)

    val additional = ss2 -- ss1
    println("\nIn actual but not in expected:")
    additional.foreach(println)

    missing.foreach(showLog(_, sim1, timestep))
    additional.foreach(showLog(_, sim2, timestep))
  }

  private def showLog(model: ModelDescriptor[_], sim1: DroneWorldSimulator, timestep: Int): Unit = {
    val log =
      for {
        log <- sim1.debugLog
        position = Vector2(model.position.x, model.position.y)
        droneID <- log.findDrone(timestep, position)
      } yield (droneID, log.retrieve(timestep - 150, timestep, droneID))
    log match {
      case Some((id, records)) =>
        println(s"\nLog for $id")
        records.foreach {
          case (t, Position(pos, angle)) => //println(f"[$t] (${pos.x}%.3f, ${pos.y}%.3f), $angle")
          case (t, Command(c, redundant)) => println(s"[$t] ${if (redundant) s"($c)" else s"$c"}")
          case (t, DamageTaken(damage, finalHealth)) => println(s"[$t] Health ${finalHealth+damage} -> $finalHealth")
          case (t, UnstructuredEvent(msg)) => println(s"[$t] $msg")
          case (t, x) => println(s"[$t] $x")
        }
      case None =>
        println(s"\nFailed to obtain detailed logs for $model")
    }
  }
}

