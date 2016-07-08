package cwinter.codecraft.core.replay

import cwinter.codecraft.core.api.{DroneSpec, Player}
import cwinter.codecraft.core.game.WorldMap
import cwinter.codecraft.core.objects.drone.DroneCommand
import cwinter.codecraft.util.maths.{Rectangle, Vector2}


private[codecraft] object NullReplayRecorder extends ReplayRecorder {
  override def recordInitialWorldState(map: WorldMap, rngSeed: Int): Unit = ()
  override def recordVersion(): Unit = ()
  override def recordSpawn(droneSpec: DroneSpec, position: Vector2, player: Player, resources: Int, name: Option[String]): Unit = ()
  override def recordWorldSize(rectangle: Rectangle): Unit = ()
  override def record(droneID: Int, droneCommand: DroneCommand): Unit = ()
  override def writeLine(string: String): Unit = ()
}
