package cwinter.codinggame.core.replay

import cwinter.codinggame.core.api.DroneSpec
import cwinter.codinggame.core.objects.drone.DroneCommand
import cwinter.codinggame.util.maths.Vector2
import cwinter.codinggame.worldstate.Player


trait ReplayRecorder {
  var timestep: Long = 0
  var timestepWritten: Boolean = false

  def newTimestep(t: Long): Unit = {
    timestep = t
    timestepWritten = false
  }

  def recordVersion(): Unit = {
    writeLine("Version=0.1")
  }

  def record(droneID: Int, droneCommand: DroneCommand): Unit = {
    if (!timestepWritten) {
      writeLine("Timestep=" + timestep)
      timestepWritten = true
    }
    writeLine(s"$droneID!$droneCommand")
  }

  def recordSpawn(droneSpec: DroneSpec, position: Vector2, player: Player): Unit = {
    writeLine("Spawn")
    writeLine(s"Spec=$droneSpec")
    writeLine(s"Position=$position")
    writeLine(s"Player=${player.id}")
  }

  def recordRngSeed(rngSeed: Int): Unit = {
    writeLine(s"Seed=$rngSeed")
  }

  protected def writeLine(string: String): Unit
}
