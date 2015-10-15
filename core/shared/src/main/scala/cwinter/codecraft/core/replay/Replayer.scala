package cwinter.codecraft.core.replay

import cwinter.codecraft.core.Spawn
import cwinter.codecraft.core.api.{Player, DroneSpec}
import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.objects.drone.{DroneCommand, DroneImpl}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}

private[core] class Replayer(lines: Iterator[String]) {
  def readLine: String = lines.next()

  final val KeyValueRegex = "(\\w*?)=(.*)".r
  final val CommandRegex = "(\\d*?)!(.*)".r


  private[this] var currLine: String = null
  private[this] def nextLine: String = {
    currLine = readLine
    currLine
  }


  // parse version
  val header = nextLine
  require(header == "ReplayVersion=0.1.2", "Incorrect replay version.")

  // parse rng seed
  val KeyValueRegex("Seed", AsInt(seed)) = nextLine

  private val KeyValueRegex("Size", worldSizeStr) = nextLine
  val worldSize = Rectangle.fromString(worldSizeStr)

  private[this] var _startingMinerals = List.empty[MineralCrystalImpl]
  while (nextLine.startsWith("Mineral")) {
    val KeyValueRegex("Mineral", mcStr) = currLine
    _startingMinerals ::= MineralCrystalImpl.fromString(mcStr)
  }
  val startingMinerals = _startingMinerals

  // parse spawns
  var spawns = List.empty[Spawn]
  //noinspection LoopVariableNotUpdated  (actually is updated whenever nextLine is called)
  while (currLine == "Spawn") {
    val KeyValueRegex("Spec", specString) = nextLine
    val spec = DroneSpec(specString)
    val KeyValueRegex("Position", Vector2(position)) = nextLine
    val KeyValueRegex("Player", AsInt(playerID)) = nextLine
    val player = Player.fromID(playerID)
    spawns ::= Spawn(spec, new DummyDroneController, position, player)
  }

  private[this] var currTime: Int = 0
  private[core] def run(timestep: Int)(implicit droneRegistry: Map[Int, DroneImpl], mineralRegistry: Map[Int, MineralCrystalImpl]): Unit = {
    while (currTime <= timestep && lines.hasNext) {
      nextLine match {
        case KeyValueRegex("Timestep", AsInt(t)) => currTime = t
        case CommandRegex(AsInt(droneID), DroneCommand(d)) =>
          droneRegistry(droneID).executeCommand(d)
        case t => throw new Exception(s"Could not parse line: $t")
      }
    }
  }


  def finished = !lines.hasNext
}
