package cwinter.codecraft.core.replay

import cwinter.codecraft.core.api.{MetaController, DroneControllerBase, DroneController, Player}
import cwinter.codecraft.core.game._
import cwinter.codecraft.core.objects.drone.DroneCommand
import upickle.default._

import scala.collection.mutable.ListBuffer

class Replayer(lines: Iterator[String]) {
  def readLine: String = lines.next()

  private[this] var lineNumber: Int = -1
  private[this] var currRecord: ReplayRecord = _
  private[this] var currLine: String = _
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
  private[this] val _startingMinerals = ListBuffer.empty[MineralSpawn]
  var spawns = Seq.empty[Spawn]
  var mineralCount = 0
  nextRecord()
  while (currRecord match {
           case SpawnRecord(spec, position, playerID, resources, name) =>
             spawns :+= Spawn(spec, position, Player.fromID(playerID), resources, name)
             true
           case MineralRecord(size, position) =>
             _startingMinerals.append(MineralSpawn(size, position))
             mineralCount += 1
             true
           case _ =>
             false
         }) {
    nextRecord()
  }
  val startingMinerals: Seq[MineralSpawn] = _startingMinerals.toList

  def controllers = spawns.map(_ => new DummyDroneController)

  val map = WorldMap(startingMinerals, worldSize, spawns)

  private[this] var currTime: Long = 0
  private[core] def run(timestep: Long)(implicit context: SimulationContext): Unit = {
    while (currTime <= timestep && lines.hasNext) {
      nextRecord()
      currRecord match {
        case Timestep(t) =>
          currTime = t
        case c @ Command(droneID, d) =>
          if (!context.droneRegistry.contains(droneID)) {
            println(s"[t=$currTime] Trying to execute $c, but drone doe not exist!")
          }
          context.drone(droneID).executeCommand(DroneCommand(d))
        case t =>
          throw new Exception(
            s"""Error while parsing replay. Expected a "Timestep" or "Command" on line $lineNumber, instead: $currLine""")
      }
    }
  }

  def finished: Boolean = !lines.hasNext

  def gameConfig = map.createGameConfig(
    droneControllers = map.initialDrones.map(_ => replayDroneController()),
    rngSeed = seed
  )

  private var metaControllerExists = false
  private def replayDroneController() =
    if (metaControllerExists) new DummyDroneController
    else {
      metaControllerExists = true
      new DroneControllerBase { override def metaController = Some(ReplayMetaController) }
    }

  private object ReplayMetaController extends MetaController {
    override def onTick(): Unit = run(_simulationContext.timestep)
  }
}
