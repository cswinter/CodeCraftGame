package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.objects.drone.{DroneCommand, DroneStateMessage}

import scala.concurrent.Future


private[core] trait RemoteClient {
  def waitForCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
  def sendWorldState(worldState: Iterable[DroneStateMessage]): Unit
  def players: Set[Player]
}

