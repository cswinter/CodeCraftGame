package cwinter.codecraft.core.replay

import cwinter.codecraft.core.objects.MineralCrystalImpl
import cwinter.codecraft.core.objects.drone.{DroneImpl, DroneCommand}
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
  assert(currLine == "Spawn")
  assert(nextLine.startsWith("Spec"))
  val KeyValueRegex("Position", Vector2(spawn1)) = nextLine
  assert(nextLine.startsWith("Player"))

  assert(nextLine == "Spawn")
  assert(nextLine.startsWith("Spec"))
  val KeyValueRegex("Position", Vector2(spawn2)) = nextLine
  assert(nextLine.startsWith("Player"))

  val spawns = Seq(spawn1, spawn2)


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



object AsInt {
  def unapply(s: String) = try {
    Some(s.toInt)
  } catch {
    case e: NumberFormatException => None
  }
}


object AsDouble {
  def unapply(s: String) = try {
    Some(s.toDouble)
  } catch {
    case e: NumberFormatException => None
  }
}
