package cwinter.codecraft.core.replay

import cwinter.codecraft.core.api.{Player, DroneSpec}
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.objects.drone.DroneCommand
import cwinter.codecraft.util.maths.{Rectangle, Vector2}


private[core] trait ReplayRecorder {
  var timestep: Long = 0
  var timestepWritten: Boolean = false

  def newTimestep(t: Long): Unit = {
    timestep = t
    timestepWritten = false
  }

  def recordVersion(): Unit = {
    writeLine("ReplayVersion=0.1.2")
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

  def recordWorldSize(rectangle: Rectangle): Unit = {
    writeLine(s"Size=$rectangle")
  }

  def recordMineral(mineral: MineralCrystalImpl): Unit = {
    writeLine(s"Mineral=${mineral.asString}")
  }

  def recordRngSeed(rngSeed: Int): Unit = {
    writeLine(s"Seed=$rngSeed")
  }

  def replayFilepath: Option[String] = None

  protected def writeLine(string: String): Unit
}
