package cwinter.codecraft.core.replay

import cwinter.codecraft.core.api.{Player, DroneSpec}
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.objects.drone.{SerializableDroneCommand, DroneCommand}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import upickle.default._



private sealed trait ReplayRecord
@key("Version") private case class ReplayVersion(version: String) extends ReplayRecord
@key("Cmd") private case class Command(droneID: Int, command: SerializableDroneCommand) extends ReplayRecord
@key("Time") private case class Timestep(time: Long) extends ReplayRecord
@key("Spawn") private case class SpawnRecord(spec: DroneSpec, position: Vector2, playerID: Int, resources: Int, name: Option[String]) extends ReplayRecord
@key("WorldSize") private case class WorldSize(size: Rectangle) extends ReplayRecord
@key("Mineral") private case class MineralRecord(size: Int, position: Vector2) extends ReplayRecord
private object MineralRecord {
  def apply(impl: MineralCrystalImpl): MineralRecord = MineralRecord(impl.size, impl.position)
}
@key("Seed") private case class RNGSeed(seed: Int) extends ReplayRecord


private[core] trait ReplayRecorder {
  var timestep: Long = 0
  var timestepWritten: Boolean = false

  def newTimestep(t: Long): Unit = {
    timestep = t
    timestepWritten = false
  }

  def recordVersion(): Unit =
    writeRecord(ReplayVersion("0.2.0"))

  def record(droneID: Int, droneCommand: DroneCommand): Unit = {
    if (!timestepWritten) {
      writeRecord(Timestep(timestep))
      timestepWritten = true
    }
    writeRecord(Command(droneID, droneCommand.toSerializable))
  }

  def recordSpawn(droneSpec: DroneSpec, position: Vector2, player: Player, resources: Int, name: Option[String]): Unit =
    writeRecord(SpawnRecord(droneSpec, position, player.id, resources, name))

  def recordWorldSize(rectangle: Rectangle): Unit =
    writeRecord(WorldSize(rectangle))

  def recordMineral(mineral: MineralCrystalImpl): Unit =
    writeRecord(MineralRecord(mineral))

  def recordRngSeed(rngSeed: Int): Unit =
    writeRecord(RNGSeed(rngSeed))

  def replayFilepath: Option[String] = None

  private def writeRecord(record: ReplayRecord): Unit =
    writeLine(write(record))

  protected def writeLine(string: String): Unit
}

