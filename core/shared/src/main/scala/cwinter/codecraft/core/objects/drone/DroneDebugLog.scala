package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.util.maths.Vector2


private[codecraft] class DroneDebugLog {
  private[this] var events = List.empty[Record]


  def record(time: Int, droneID: Int, datum: DebugLogDatum): Unit =
    events ::= Record(time, droneID, datum)

  def retrieve(begin: Int, end: Int, droneID: Int): Seq[(Int, DebugLogDatum)] = {
    for {
      Record(t, id, datum) <- events
      if begin <= t && t <= end && droneID == id
    } yield (t, datum)
  }.reverse

  def findDrone(time: Int, location: Vector2): Option[Int] = {
    events.find {
      case Record(t, _, Position(pos, _)) => pos == location
      case _ => false
    }
  }.map(_.droneID)

  private case class Record(time: Int, droneID: Int, event: DebugLogDatum)
}


private[codecraft] sealed trait DebugLogDatum
private[codecraft] case class Position(pos: Vector2, orientation: Float) extends DebugLogDatum
private[codecraft] case class Command(droneCommand: DroneCommand, redundant: Boolean) extends DebugLogDatum
private[codecraft] case class Collision(position: Vector2, otherDroneID: Int) extends DebugLogDatum
private[codecraft] case class DamageTaken(damage: Int, finalHealth: Int) extends DebugLogDatum
private[codecraft] case class UnstructuredEvent(message: String) extends DebugLogDatum

