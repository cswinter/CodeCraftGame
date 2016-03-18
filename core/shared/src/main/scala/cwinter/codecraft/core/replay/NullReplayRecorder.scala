package cwinter.codecraft.core.replay

import cwinter.codecraft.core.MineralSpawn
import cwinter.codecraft.core.objects.drone.DroneCommand


private[codecraft] object NullReplayRecorder extends ReplayRecorder {
  override def recordMineral(mineral: MineralSpawn): Unit = ()
  override def record(droneID: Int, droneCommand: DroneCommand): Unit = ()
  override def writeLine(string: String): Unit = ()
}
