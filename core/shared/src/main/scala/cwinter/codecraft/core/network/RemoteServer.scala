package cwinter.codecraft.core.network

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.objects.drone.{DroneCommand, DroneDynamicsState}

private[core] trait RemoteServer {
  def receiveCommands()(implicit context: SimulationContext): Seq[(Int, DroneCommand)]
  def receiveWorldState(): Iterable[DroneDynamicsState]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
}
