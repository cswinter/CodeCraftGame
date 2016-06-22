package cwinter.codecraft.core

import cwinter.codecraft.core.graphics.DroneModel
import cwinter.codecraft.graphics.engine.ModelDescriptor

import scala.collection.mutable.ArrayBuffer


object TestUtils {
  type GameRecord = IndexedSeq[Set[ModelDescriptor[_]]]


  def runAndRecord(droneWorldSimulator: DroneWorldSimulator, timesteps: Int): GameRecord = {
    val snapshots = ArrayBuffer.empty[Set[ModelDescriptor[_]]]

    for (i <- 0 to timesteps) {
      droneWorldSimulator.run(1)
      snapshots.append(droneWorldSimulator.worldState.filter{
        case ModelDescriptor(pos, descriptor, params) => descriptor.isInstanceOf[DroneModel]
      }.toSet)
    }

    snapshots
  }

  def assertEqual(run1: GameRecord, run2: GameRecord, debugInfo: String): Unit = {
    require(run1.size == run2.size)
    val timesteps = run1.size
    for (t <- 0 until timesteps) {
      if (run1(t) != run2(t)) {
        println(debugInfo)
        println(s"Games diverged at t=$t\n")
        showMismatch(run1(t), run2(t))
      }
    }
  }

  private def showMismatch(snapshot1: Set[ModelDescriptor[_]], snapshot2: Set[ModelDescriptor[_]]): Unit = {
    println("Expected:")
    println(snapshot1)
    println()
    println("Actual:")
    println(snapshot2)
    println()
    println("In expected but not in actual:")
    (snapshot1 -- snapshot2).foreach(println)
    println()
    println("In actual but not in expected:")
    (snapshot2 -- snapshot1).foreach(println)
    assert(false)
  }
}
