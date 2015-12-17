package cwinter.codecraft.core.replay

import cwinter.codecraft.core.{WorldMap, Spawn}
import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.objects.drone.{DroneCommand, DroneImpl}
import upickle.default._

class Replayer(lines: Iterator[String]) {
  def readLine: String = lines.next()

  private[this] var lineNumber: Int = -1
  private[this] var currRecord: ReplayRecord = null
  private[this] var currLine: String = null
  private[this] def nextLine(): String = {
    lineNumber += 1
    currLine = readLine
    currLine
  }
  private[this] def nextRecord(): ReplayRecord = {
    val line = nextLine()
    currRecord = read[ReplayRecord](line)
    currRecord
  }


  // read version
  val ReplayVersion(version) = read[ReplayVersion](nextLine())
  require(version == Replay.CurrentVersion, "Incorrect replay version.")

  // read rng seed
  val RNGSeed(seed) = read[RNGSeed](nextLine())

  // read world size
  val WorldSize(worldSize) = read[WorldSize](nextLine())

  // load initial spawns and mineral crystals
  private[this] var _startingMinerals = List.empty[MineralCrystalImpl]
  var spawns = Seq.empty[Spawn]
  var mineralCount = 0
  nextRecord()
  while (
    currRecord match {
      case SpawnRecord(spec, position, playerID, resources, name) =>
        spawns :+= Spawn(spec, position, Player.fromID(playerID), resources, name)
        true
      case MineralRecord(size, position) =>
        _startingMinerals ::= new MineralCrystalImpl(size, mineralCount, position)
        mineralCount += 1
        true
      case _ =>
        false
    }
  ) {
    nextRecord()
  }
  val startingMinerals = _startingMinerals

  def controllers = spawns.map(_ => new DummyDroneController)

  val map = WorldMap(startingMinerals, worldSize, spawns)


  private[this] var currTime: Long = 0
  private[core] def run(
    timestep: Long
  )(implicit
    droneRegistry: Map[Int, DroneImpl],
    mineralRegistry: Map[Int, MineralCrystalImpl]
  ): Unit = {
    while (currTime <= timestep && lines.hasNext) {
      nextRecord()
      currRecord match {
        case Timestep(t) =>
          currTime = t
        case Command(droneID, d) =>
          droneRegistry(droneID).executeCommand(DroneCommand(d))
        case t => throw new Exception(s"""Error while parsing replay. Expected a "Timestep" or "Command" on line $lineNumber, instead: $currLine""")
      }
    }
  }


  def finished = !lines.hasNext
}

