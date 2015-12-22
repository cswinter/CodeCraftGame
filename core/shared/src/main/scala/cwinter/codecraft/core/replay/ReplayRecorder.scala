package cwinter.codecraft.core.replay

import cwinter.codecraft.core.{MineralSpawn, Spawn, WorldMap}
import cwinter.codecraft.core.api.{Player, DroneSpec}
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.objects.drone.{SerializableDroneCommand, DroneCommand}
import cwinter.codecraft.util.maths.{Rng, Rectangle, Vector2}
import upickle.default._



private sealed trait ReplayRecord
@key("Version") private case class ReplayVersion(version: String) extends ReplayRecord
@key("Cmd") private case class Command(droneID: Int, command: SerializableDroneCommand) extends ReplayRecord
@key("Time") private case class Timestep(time: Long) extends ReplayRecord
@key("Spawn") private case class SpawnRecord(spec: DroneSpec, position: Vector2, playerID: Int, resources: Int, name: Option[String]) extends ReplayRecord
@key("WorldSize") private case class WorldSize(size: Rectangle) extends ReplayRecord
@key("Mineral") private case class MineralRecord(size: Int, position: Vector2) extends ReplayRecord
private object MineralRecord {
  def apply(mineral: MineralSpawn): MineralRecord = MineralRecord(mineral.size, mineral.position)
}
@key("Seed") private case class RNGSeed(seed: Int) extends ReplayRecord


private[core] trait ReplayRecorder {
  var timestep: Long = 0
  var timestepWritten: Boolean = false

  def newTimestep(t: Long): Unit = {
    timestep = t
    timestepWritten = false
  }

  def recordInitialWorldState(map: WorldMap): Unit = {
    recordVersion()
    recordRngSeed(Rng.seed)
    recordWorldSize(map.size)
    map.minerals.foreach(recordMineral)
    for (Spawn(spec, position, player, resources, name) <- map.initialDrones)
      recordSpawn(spec, position, player, resources, name)
  }

  def recordVersion(): Unit =
    writeRecord(ReplayVersion(Replay.CurrentVersion))

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

  def recordMineral(mineral: MineralSpawn): Unit =
    writeRecord(MineralRecord(mineral))

  def recordRngSeed(rngSeed: Int): Unit =
    writeRecord(RNGSeed(rngSeed))

  def replayString: Option[String] = None

  def replayFilepath: Option[String] = None

  private def writeRecord(record: ReplayRecord): Unit =
    writeLine(write(record))

  protected def writeLine(string: String): Unit
}

