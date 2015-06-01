package cwinter.codinggame.core.replay

import cwinter.codinggame.util.maths.{Rng, Vector2}


trait Replayer {
  def readLine: String

  final val KeyValueRegex = "(\\w*?)=(.*)".r



  private[this] var currLine: String = null
  private[this] def nextLine: String = {
    currLine = readLine
    currLine
  }


  // parse version
  val header = nextLine
  assert(header == "Version=0.1")

  // parse rng seed
  val KeyValueRegex("Seed", AsInt(seed)) = nextLine


  // parse spawns
  assert(nextLine == "Spawn")
  assert(nextLine.startsWith("Spec"))
  val KeyValueRegex("Position", Vector2(spawn1)) = nextLine
  assert(nextLine == "Spawn")
  assert(nextLine.startsWith("Spec"))
  val KeyValueRegex("Position", Vector2(spawn2)) = nextLine
  val spawns = Seq(spawn1, spawn2)


}



object AsInt {
  def unapply(s: String) = try {
    Some(s.toInt)
  } catch {
    case e: NumberFormatException => None
  }
}
