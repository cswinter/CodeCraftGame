package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.objects.drone.{DroneStateMessage, DroneCommand}

private[core] trait RemoteServer {
  def receiveCommands()(implicit context: SimulationContext): Seq[(Int, DroneCommand)]
  def receiveWorldState(): Iterable[DroneStateMessage]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
}
