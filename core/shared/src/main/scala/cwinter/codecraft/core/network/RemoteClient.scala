package cwinter.codecraft.core.network

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.objects.drone.{DroneCommand, DroneStateMessage}


private[core] trait RemoteClient {
  def waitForCommands()(implicit context: SimulationContext): Seq[(Int, DroneCommand)]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
  def sendWorldState(worldState: Iterable[DroneStateMessage]): Unit
  def players: Set[Player]
}

