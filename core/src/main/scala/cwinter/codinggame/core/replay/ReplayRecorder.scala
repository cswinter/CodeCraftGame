package cwinter.codinggame.core.replay

import cwinter.codinggame.core.objects.drone.DroneCommand


trait ReplayRecorder {
  var timestep: Long = 0
  var timestepWritten: Boolean = false

  def newTimestep(t: Long): Unit = {
    if (t == 0) writeLine("Version=0.1")
    timestep = t
    timestepWritten = false
  }

  def record(droneID: Int, droneCommand: DroneCommand): Unit = {
    if (!timestepWritten) {
      writeLine("Timestep=" + timestep)
      timestepWritten = true
    }
    writeLine(s"$droneID ! $droneCommand")
  }


  protected def writeLine(string: String): Unit
}
