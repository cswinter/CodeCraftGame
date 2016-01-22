package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.objects.drone.{DroneStateMessage, DroneCommand}

import scala.concurrent.Future

private[core] trait RemoteServer {
  def receiveCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]]
  def receiveWorldState(): Future[Iterable[DroneStateMessage]]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
}
